import * as THREE from 'three'
import { MeshStandardMaterial } from 'three'
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js'
import type { GLTF } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js'
import type { Ref } from 'vue'
import { computed, reactive, ref, watch } from 'vue'
import { searchFilter } from '@/main'
import { cloneMeshMaterial } from '@/components/3D/util'
import { removePin } from '@/components/3D/pin_goal'
import { replaceUnassignedPinByPinForRobot } from '@/components/3D/click_handling'

const loader = new GLTFLoader()

let scene: THREE.Scene

export function handleRobotPosSocket(host: string, scene_: THREE.Scene) {
  scene = scene_
  const robotPosSocket = new WebSocket(`ws://${host}/ws/robot-pos`)

  robotPosSocket.addEventListener('open', () => {
    console.log('robotPosSocket connected')
    robotPosSocket.send('sendall')
  })

  robotPosSocket.addEventListener('message', (event) => {
    handleRobotPosUpdate(event.data, scene)
  })
}

type RobotPos = {
  x: number
  y: number
  angle: number

}

type RobotData = {
  last?: RobotPos,
  current: RobotPos,
  transitionStartTime?: number
}

let robotsAndPos: { [key: string]: RobotData } = reactive({})

// const times: number[] = []
// let lastSendTime = 0

function handleRobotPosUpdate(update: string, scene: THREE.Scene) {
  // if (lastSendTime !== 0) {
  //   times.push(Date.now() - lastSendTime)
  //   if (times.length > 20) {
  //     times.shift()
  //     console.log('Average time between robot pos updates', times.reduce((a, b) => a + b) / times.length)
  //   }
  //   console.log('Time between robot pos updates', Date.now() - lastSendTime)
  // }
  // lastSendTime = Date.now()

  const split = update.split(' ')
  const robotId = split[0]
  if (split[1] === 'remove') {
    // we only have the robot id, this means we should remove the robot from the scene
    scene.getObjectByName(`robot-${robotId}`)?.removeFromParent()
    delete robotsAndPos[robotId]
    document.getElementById('robot-label-' + robotId)?.remove()
    removePin(robotId, false)
  } else {
    const coords = split[1].split(',')
    let x = parseFloat(coords[0])
    let y = parseFloat(coords[1])
    let angle = parseFloat(coords[2])

    let transitionStartTime = Date.now()

    const robot = createRobotIfNotExists(robotId, scene)
    if (robot !== undefined) {
      const lastAngle = robotsAndPos[robotId]?.current.angle
      if (lastAngle !== undefined) {
        // make sure the robot turns the right way
        const angleDistance = angle - lastAngle
        if (angleDistance > Math.PI) {
          angle -= 2 * Math.PI
        } else if (angleDistance < -Math.PI) {
          angle += 2 * Math.PI
        }
      }
      robotsAndPos[robotId] = {
        last: robotsAndPos[robotId]?.current,
        current: { x, y, angle },
        transitionStartTime
      }
    }
  }
}

// should match the rate of sent robot positions
const TRANSITION_DURATION = 400 // ms

const ROBOT_Z = 0.045

const ROBOT_OSCILLATION_PERIOD = 500 // ms

export function updateRobots(scene: THREE.Scene) {
  scene.traverse((object) => {
    if (object.name.startsWith('robot-')) {
      const robotId = object.name.substring(6) // 6 is the length of 'robot-'
      const pos = robotsAndPos[robotId]
      if (pos !== undefined) {
        if (pos.last !== undefined && pos.transitionStartTime !== undefined) {
          // transition
          const progress = Math.min((Date.now() - pos.transitionStartTime) / TRANSITION_DURATION, 1)
          const distanceX = pos.current.x - pos.last.x
          const distanceY = pos.current.y - pos.last.y
          const angleDistance = pos.current.angle - pos.last.angle
          const x = pos.last.x + distanceX * progress
          const y = pos.last.y + distanceY * progress
          const angle = pos.last.angle + angleDistance * progress
          object.position.set(x, y, ROBOT_Z + 0.004 * Math.sin(2 * Math.PI * Date.now() / ROBOT_OSCILLATION_PERIOD))
          object.rotation.set(Math.PI / 2, angle + Math.PI / 2, 0)

          // make wheels turn
          const frontWheels = object.getObjectByName('wheels-front')
          const backWheels = object.getObjectByName('wheels-back')
          if (frontWheels !== undefined && backWheels !== undefined) {
            const movementAngle = Math.atan2(distanceY, distanceX) - angle
            let rotation = Math.sqrt(distanceX ** 2 + distanceY ** 2) * Math.cos(movementAngle) * 0.35
            // console.log('rotation', rotation, distanceForAngle, distanceX, distanceY, movementAngle, angle, pos.last.angle, pos.current.angle)
            frontWheels.rotation.x += rotation
            backWheels.rotation.x += rotation
          }
        } else {
          // no transition
          object.position.set(pos.current.x, pos.current.y, ROBOT_Z + 0.004 * Math.sin(2 * Math.PI * Date.now() / ROBOT_OSCILLATION_PERIOD))
          object.rotation.set(Math.PI / 2, pos.current.angle + Math.PI / 2, 0)
        }
      }
    }
  })
}

function distance(a: RobotPos, b: RobotPos) {
  return Math.sqrt((a.x - b.x) ** 2 + (a.y - b.y) ** 2)
}

let robotObject: THREE.Object3D

loader.load('/3D_models/robot.glb', (gltf: GLTF) => {
  const obj = gltf.scene
  // position and rotation are not set here, but we do resize the object, so it has thr right size
  let scale = 0.25
  obj.scale.set(scale, scale, scale)
  obj.traverse(function(child) {
    if ((child as THREE.Mesh).isMesh) { // see https://discourse.threejs.org/t/gltf-scene-traverse-property-ismesh-does-not-exist-on-type-object3d/27212
      child.castShadow = true
      child.receiveShadow = true
      switch (child.name) {
        case 'Cube003':
          child.name = 'wheels-front'
          break
        case 'Cube005':
          child.name = 'wheels-back'
          break
      }
    }
  })

  robotObject = obj
  console.log('Robot model loaded')
}, undefined, (error) => {
  console.error('Error loading robot model', error)
})

function getNewRobot(defaultRobot: THREE.Object3D, robotId: string): THREE.Object3D {
  const obj = defaultRobot.clone()
  obj.traverse(child => {
    if ((child as THREE.Mesh).isMesh) {
      const mesh = child as THREE.Mesh
      cloneMeshMaterial(mesh)
    }

  })

  obj.name = `robot-${robotId}`

  // label
  const div = document.createElement('div')
  div.className = 'robot-label'
  div.id = 'robot-label-' + robotId
  div.textContent = robotId
  const label = new CSS2DObject(div)
  label.position.set(0, 1.4, 0)
  obj.add(label)

  return obj
}

/**
 * Create a robot if it does not exist in the scene
 * @return the robot object (inside a promise because the loading is async)
 */
function createRobotIfNotExists(
  robotId: string,
  scene: THREE.Scene
): THREE.Object3D | undefined {
  const sceneRobot: THREE.Object3D | undefined = scene.getObjectByName(`robot-${robotId}`)

  if (sceneRobot === undefined) {
    if (robotObject !== undefined) {
      const robot = getNewRobot(robotObject, robotId)
      scene.add(robot)
      console.log('Robot created ' + robotId)
      return robot
    } else {
      console.log('Robot model not loaded yet, skipping robot creation')
      return undefined
    }
  } else {
    return sceneRobot
  }
}

export function resetRobots() {
  robotsAndPos = {}
}

export let selectedRobot: Ref<string | null> = ref(null)

let blinkInterval: number | undefined
let blinkIsOn = false

watch(selectedRobot, (newVal, oldVal) => {
  console.log('selectedRobot', newVal, oldVal)
  if (blinkInterval !== undefined) {
    clearInterval(blinkInterval)
    blinkInterval = undefined
  }
  if (oldVal !== null) {
    document.getElementById('robot-label-' + oldVal)?.classList.remove('selected')
    setRobotEmissivity(oldVal, 0x000000)
  }
  blinkIsOn = true
  if (newVal !== null) {
    document.getElementById('robot-label-' + newVal)?.classList.add('selected')
    blinkRobot(newVal)
    if (Date.now() - lastSearchFilterChanged >= 500) {
      searchFilter.value = newVal
    }
  } else {
    if (Date.now() - lastSearchFilterChanged >= 500) {
      searchFilter.value = ''
    }
  }
})

let lastSearchFilterChanged = 0

export function searchFilterEnterKey() {
  selectRobotWithSearchFilter()
  if (selectedRobot.value) {
    replaceUnassignedPinByPinForRobot(selectedRobot.value, scene)
    searchFilter.value = selectedRobot.value
  }
}

function selectRobotWithSearchFilter() {
  let newSearchFilter = searchFilter.value
  console.log('triggered searchFilter watch', newSearchFilter)
  lastSearchFilterChanged = Date.now()
  if (newSearchFilter === '') {
    selectedRobot.value = null
    return
  }
  for (const robotId in robotsAndPos) {
    if (robotId.includes(newSearchFilter)) {
      selectedRobot.value = robotId
      return
    }
  }
}

export function initSearchFilterWatching() {
  watch(searchFilter, selectRobotWithSearchFilter, { immediate: true })
  watch(robotCount, selectRobotWithSearchFilter)
}

function setRobotEmissivity(robotId: string, colorHex: number) {
  scene.getObjectByName(`robot-${robotId}`)?.traverse((child: THREE.Object3D) => {
    if ((child as THREE.Mesh).isMesh) {
      const mesh = child as THREE.Mesh
      (mesh.material as MeshStandardMaterial).emissive.setHex(colorHex)
    }
  })
}

function blinkRobot(robotId: string) {
  let label = document.getElementById('robot-label-' + robotId)
  if (label) {
    label?.classList.add('selected')
  }
  let colorHex = blinkIsOn ? 0xa0a0a0 : 0x000000
  setRobotEmissivity(robotId, colorHex)
  blinkIsOn = !blinkIsOn
  blinkInterval = setTimeout(() => {
    blinkRobot(robotId)
  }, 500)
}

export const robotCount = computed(() => Object.keys(robotsAndPos).length)

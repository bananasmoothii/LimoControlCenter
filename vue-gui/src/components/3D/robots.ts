import * as THREE from 'three'
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js'
import type { GLTF } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js'

const loader = new GLTFLoader()

export function handleRobotPosSocket(host: string, scene: THREE.Scene) {
  const robotPosSocket = new WebSocket(`ws://${host}/ws/robot-pos`)

  robotPosSocket.addEventListener('open', () => {
    console.log('robotPosSocket connected')
    robotPosSocket.send('sendall')
  })

  robotPosSocket.addEventListener('message', (event) => {
    console.log('robotPosSocket message', event.data)

    handleRobotPosUpdate(event.data, scene)
  })
}

type RobotPos = {
  x: number
  y: number
  angle: number

}

let robotsAndPos: { [key: string]: { last?: RobotPos, current: RobotPos, transitionStartTime?: number } } = {}

function handleRobotPosUpdate(update: string, scene: THREE.Scene) {
  const split = update.split(' ')
  const robotId = split[0]
  if (split[1] === 'remove') {
    // we only have the robot id, this means we should remove the robot from the scene
    scene.getObjectByName(`robot-${robotId}`)?.removeFromParent()
    delete robotsAndPos[robotId]
    document.getElementById('robot-label-' + robotId)?.remove()
  } else {
    const coords = split[1].split(',')
    const x = parseFloat(coords[0])
    const y = parseFloat(coords[1])
    const angle = parseFloat(coords[2])

    let transitionStartTime = Date.now()

    createRobotIfNotExists(robotId, scene).then(robotObject => {
      if (robotObject !== undefined) {
        robotsAndPos[robotId] = {
          last: robotsAndPos[robotId]?.current,
          current: { x, y, angle },
          transitionStartTime
        }
      }
    })
  }
}

// should match the rate of sent robot positions
const TRANSITION_DURATION = 100 // ms

const ROBOT_Y = 0.045

const ROBOT_OSCILLATION_PERIOD = 500 // ms

export function updateRobots(scene: THREE.Scene) {
  scene.traverse((object) => {
    if (object.name.startsWith('robot-')) {
      const robotId = object.name.split('-')[1]
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
          object.position.set(x, ROBOT_Y + 0.004 * Math.sin(2 * Math.PI * Date.now() / ROBOT_OSCILLATION_PERIOD), y)
          object.rotation.set(0, angle + Math.PI / 2, 0)

          // make wheels turn
          const frontWheels = object.getObjectByName('wheels-front')
          const backWheels = object.getObjectByName('wheels-back')
          if (frontWheels !== undefined && backWheels !== undefined) {
            const movementAngle = Math.atan2(distanceY, distanceX) - angle
            const distanceForAngle = Math.sqrt(distanceX ** 2 + distanceY ** 2) * Math.cos(movementAngle)
            let rotation = distanceForAngle
            // console.log('rotation', rotation, distanceForAngle, distanceX, distanceY, movementAngle, angle, pos.last.angle, pos.current.angle)
            frontWheels.rotation.x += rotation
            backWheels.rotation.x += rotation
          }
        } else {
          // no transition
          object.position.set(pos.current.x, ROBOT_Y, pos.current.y)
          object.rotation.set(0, pos.current.angle + Math.PI / 2, 0)
        }
      }
    }
  })
}

function distance(a: RobotPos, b: RobotPos) {
  return Math.sqrt((a.x - b.x) ** 2 + (a.y - b.y) ** 2)
}

/**
 * Create a robot if it does not exist in the scene
 * @return the robot object (inside a promise because the loading is async)
 */
async function createRobotIfNotExists(
  robotId: string,
  scene: THREE.Scene
): Promise<THREE.Object3D | undefined> {
  return new Promise((resolve, reject) => {
    const robotObject: THREE.Object3D | undefined = scene.getObjectByName(`robot-${robotId}`)

    if (robotObject === undefined) {
      if (robotId in robotsAndPos) {
        console.warn('Robot already exists in robotsAndPos', robotId)
        resolve(undefined)
      } else { // avoid creating the same robot twice
        robotsAndPos[robotId] = { current: { x: 0, y: ROBOT_Y, angle: 0 } }
        loader.load('/3D_models/robot.glb', (gltf: GLTF) => {
          const robotObject = gltf.scene
          robotObject.name = `robot-${robotId}`
          // position and rotation are not set here, but we do resize the object so it has thr right size
          let scale = 0.22
          robotObject.scale.set(scale, scale, scale)
          robotObject.traverse(function(child) {
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

          // label
          const div = document.createElement('div')
          div.className = 'robot-label'
          div.id = 'robot-label-' + robotId
          div.textContent = robotId
          const label = new CSS2DObject(div)
          label.position.set(0, 1.5, 0)
          robotObject.add(label)

          scene.add(robotObject)
          console.log('Robot model loaded: ', robotObject.name)
          resolve(robotObject)
        }, undefined, (error) => {
          console.error('Error loading robot model', error)
          delete robotsAndPos[robotId]
          reject(error)
        })
      }
    } else {
      resolve(robotObject)
    }
  })
}

export function resetRobots() {
  robotsAndPos = {}
}

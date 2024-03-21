import * as THREE from 'three'
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js'
import type { GLTF } from 'three/examples/jsm/loaders/GLTFLoader.js'

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

const robotsAndPos: { [key: string]: { last?: RobotPos, current: RobotPos, transitionStartTime?: number } } = {}

function handleRobotPosUpdate(update: string, scene: THREE.Scene) {
  const split = update.split(' ')
  const robotId = split[0]
  if (split[1] === 'remove') {
    // we only have the robot id, this means we should remove the robot from the scene
    scene.getObjectByName(`robot-${robotId}`)?.removeFromParent()
    delete robotsAndPos[robotId]
  } else {
    const coords = split[1].split(',')
    const x = parseFloat(coords[0])
    const y = parseFloat(coords[1])
    const angle = parseFloat(coords[2])
    robotsAndPos[robotId] = {
      last: robotsAndPos[robotId]?.current,
      current: { x, y, angle },
      transitionStartTime: Date.now()
    }

    createRobotIfNotExists(robotId, scene)
  }
}

// should match the rate of sent robot positions
const TRANSITION_DURATION = 1000 // ms

const ROBOT_Y = 0.045

const ROBOT_OSCILLATION_PERIOD = 500 // ms

export function updateRobots(scene: THREE.Scene) {
  scene.traverse((child) => {
    if (child.name.startsWith('robot-')) {
      const robotId = child.name.split('-')[1]
      const pos = robotsAndPos[robotId]
      if (pos !== undefined) {
        if (pos.last !== undefined && pos.transitionStartTime !== undefined) {
          // transition
          const progress = Math.min((Date.now() - pos.transitionStartTime) / TRANSITION_DURATION, 1)
          const x = pos.last.x + (pos.current.x - pos.last.x) * progress
          const y = pos.last.y + (pos.current.y - pos.last.y) * progress
          const angle = pos.last.angle + (pos.current.angle - pos.last.angle) * progress
          child.position.set(x, ROBOT_Y + 0.005 * Math.sin(2 * Math.PI * Date.now() / ROBOT_OSCILLATION_PERIOD), y)
          child.rotation.set(0, angle, 0)
        } else {
          // no transition
          child.position.set(pos.current.x, ROBOT_Y, pos.current.y)
          child.rotation.set(0, pos.current.angle, 0)
        }
      }
    }
  })
}

/**
 * Create a robot if it does not exist in the scene
 * @return the robot object (inside a promise because the loading is async)
 */
async function createRobotIfNotExists(
  robotId: string,
  scene: THREE.Scene
): Promise<THREE.Object3D> {
  return new Promise((resolve, reject) => {
    const robotObject: THREE.Object3D | undefined = scene.getObjectByName(`robot-${robotId}`)

    if (robotObject === undefined) {
      if (robotId ! in robotsAndPos) { // avoid creating the same robot twice
        robotsAndPos[robotId] = { current: { x: 0, y: ROBOT_Y, angle: 0 } }
        loader.load('/3D_models/robot.glb', (gltf: GLTF) => {
          const robotObject = gltf.scene
          robotObject.name = `robot-${robotId}`
          // position and rotation are not set here, but we do resize the object so it has thr right size
          let scale = 0.22
          robotObject.scale.set(scale, scale, scale)
          gltf.scene.traverse(function(child) {
            if ((child as THREE.Mesh).isMesh) { // see https://discourse.threejs.org/t/gltf-scene-traverse-property-ismesh-does-not-exist-on-type-object3d/27212
              child.castShadow = true
              child.receiveShadow = true
            }
          })
          scene.add(robotObject)
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

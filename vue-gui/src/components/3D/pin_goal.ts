import type { GLTF } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import * as THREE from 'three'
import { CSS2DObject } from 'three/examples/jsm/renderers/CSS2DRenderer.js'
import { ignoreNextClick } from '@/components/3D/click_handling'

const loader = new GLTFLoader()

let defaultPinObj: THREE.Object3D | undefined = undefined

// map of robot id to pin object
export const robotGoals: { [key: string]: { obj: THREE.Object3D, isFollowing?: boolean } } = {}

export let unassignedPin: THREE.Object3D | undefined = undefined

export function loadPin(scene: THREE.Scene) {
  loader.load('/3D_models/pin_goal.glb', (gltf: GLTF) => {
    const pin = gltf.scene
    pin.traverse(child => {
      if ((child as THREE.Mesh).isMesh) {
        const mesh = child as THREE.Mesh
        mesh.castShadow = true
        mesh.receiveShadow = true
        mesh.scale.set(2, 2, 2)
        mesh.material = new THREE.MeshStandardMaterial({
          transparent: true,
          opacity: 0.6,
          metalness: 1,
          roughness: 0.5,
          emissive: 0x53a5b8, // blue // 0x78b569 for green
          side: THREE.DoubleSide
        })
      }
    })
    defaultPinObj = pin

    unassignedPin = getNewPin(null, 0xc2a9bb)
  }, undefined, (error) => {
    console.error('Error loading pin model', error)
  })
}

function getNewPin(robotId: string | null, color?: number): THREE.Object3D {
  const obj = defaultPinObj!.clone()

  if (color !== undefined) {
    obj.traverse(child => {
      if ((child as THREE.Mesh).isMesh) {
        const mesh = child as THREE.Mesh
        mesh.material = (mesh.material as THREE.Material).clone();
        (mesh.material as THREE.MeshStandardMaterial).emissive.setHex(color)
      }
    })
  }

  obj.name = robotId === null ? 'default-pin' : `pin-${robotId}`

  // label
  const button = document.createElement('button')
  let templateButton = document.getElementById('hidden-remove-pin-button')!
  button.id = 'pin-label-' + robotId
  button.className = templateButton.className
  button.innerHTML = templateButton.innerHTML
  button.style.display = 'block'
  button.style.width = '2em'
  button.style.height = '2em'
  button.addEventListener('pointerdown', (event) => {
    ignoreNextClick.value = true
    let buttonBox = button.getBoundingClientRect()
    if (buttonBox.x <= event.clientX && event.clientX <= buttonBox.x + buttonBox.width &&
      buttonBox.y <= event.clientY && event.clientY <= buttonBox.y + buttonBox.height) {

      if (robotId !== null) {
        robotGoals[robotId].obj.removeFromParent()
        delete robotGoals[robotId]
      } else {
        unassignedPin?.removeFromParent()
      }
      button.remove()
    }
  })
  const label = new CSS2DObject(button)
  label.position.set(0, 0.7, 0)
  obj.add(label)
  return obj
}

export function animatePin() {
  for (const goal of Object.values(robotGoals).concat([{ obj: unassignedPin! }])) {
    let pin = goal.obj
    if (defaultPinObj !== undefined && pin.visible) {
      pin.rotation.y = Date.now() * 0.001
      pin.position.y = 0.1 + 0.1 * Math.sin(Date.now() * 0.002)
    }
  }
}

export function getPinForSelectedRobot(robotId: string | null, scene: THREE.Scene): THREE.Object3D {
  if (robotId === null) {
    if (unassignedPin!.parent === null) {
      scene.add(unassignedPin!)
    }
    return unassignedPin!
  }
  let pin = robotGoals[robotId]?.obj
  if (pin) {
    return pin
  }
  pin = getNewPin(robotId)
  scene.add(pin)
  robotGoals[robotId] = { obj: pin }
  return pin
}

export function removeUnassignedPin() {
  if (unassignedPin) {
    unassignedPin.removeFromParent()
  }
  document.getElementById('pin-label-null')?.remove()
}

type LaunchRobotGoals = {
  [key: string]: {
    x: number,
    y: number,
  }
}

export function launchRobotsToGoals(robotIds: string[] = Object.keys(robotGoals)) {
  let robotGoalsToSend: LaunchRobotGoals = {}
  for (const [robotId, goal] of Object.entries(robotGoals)) {
    if (!robotIds.includes(robotId)) continue
    robotGoalsToSend[robotId] = { x: goal.obj.position.x, y: goal.obj.position.z }
  }
  fetch('/api/launch-robots', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ goals: robotGoalsToSend })
  }).then(response => {
    if (!response.ok) {
      console.error('Error launching robots', response)
      return
    }
    for (const robotId of robotIds) {
      let goal = robotGoals[robotId]
      goal.isFollowing = true
      goal.obj.traverse(child => {
        if ((child as THREE.Mesh).isMesh) {
          const mesh = child as THREE.Mesh
          (mesh.material as THREE.MeshStandardMaterial).emissive.setHex(0x78b569)
        }
      })
    }
  }).catch(error => {
    console.error('Error launching robots', error)
  })
}
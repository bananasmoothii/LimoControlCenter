import type { GLTF } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import * as THREE from 'three'

const loader = new GLTFLoader()

export let pinObj: THREE.Object3D | undefined = undefined

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
          emissive: 0xc2a9bb,
          side: THREE.DoubleSide
        })
      }
    })
    pinObj = pin
    pinObj.position.set(0, 0.1, 0)
    pinObj.visible = false
    scene.add(pinObj)
  }, undefined, (error) => {
    console.error('Error loading pin model', error)
  })
}

export function animatePin() {
  if (pinObj !== undefined && pinObj.visible) {
    pinObj.rotation.y = Date.now() * 0.001
    pinObj.position.y = 0.1 + 0.1 * Math.sin(Date.now() * 0.002)
  }
}
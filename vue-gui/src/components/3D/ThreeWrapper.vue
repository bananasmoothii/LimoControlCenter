<template>
  <NoWebGLDialog :display="! isWebGLAvailable" />
  <div id="three-app-wrapper">
    <div id="three-app" class="absolute top-0 left-0" :class="isGrabbing ? 'cursor-grabbing' : 'cursor-grab'"></div>
  </div>
  <button id="hidden-remove-pin-button" style="display: none;" class="pin-label">
    <XMarkIcon />
  </button>
</template>

<script lang="js">
import { defineComponent, ref } from 'vue'
import * as THREE from 'three'
import { Dialog, DialogDescription, DialogPanel, DialogTitle } from '@headlessui/vue'
import { XMarkIcon } from '@heroicons/vue/20/solid'
import NoWebGLDialog from '@/components/util/NoWebGLDialog.vue'
import WebGL from 'three/addons/capabilities/WebGL.js'
import { MapControls } from 'three/addons/controls/MapControls.js'
import { animateClouds, handleUpdateMapSockets } from './map.ts'
import { handleRobotPosSocket, resetRobots, updateRobots } from '@/components/3D/robots.ts'
import { CSS2DRenderer } from 'three/examples/jsm/renderers/CSS2DRenderer.js'
import { animatePins, handleUpdateGoalSocket, loadPin } from '@/components/3D/pin_goal.ts'
import { click_handling } from '@/components/3D/click_handling.ts'
import { host } from '@/components/3D/util.ts'

// do not put these in the data() function because they will break if in a proxy
var scene, camera, renderer, labelRenderer, controls, top
var viewWidth = ref(0)
var viewHeight = ref(0)

const ROS_CONST = 2.46

let frames = 0, prevTime = performance.now()

function initWorld() {
  THREE.Object3D.DEFAULT_UP = new THREE.Vector3(0, 0, 1)
  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000)
  camera.position.set(ROS_CONST * 0.5, 0, 2.5) // TODO: remove when the map is not offset anymore
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.VSMShadowMap
  renderer.pixelRatio = window.devicePixelRatio

  labelRenderer = new CSS2DRenderer()
  labelRenderer.domElement.style.position = 'absolute'
  labelRenderer.domElement.style.top = '0px'

  // controls: see https://threejs.org/docs/#examples/en/controls/MapControls
  controls = new MapControls(camera, labelRenderer.domElement)
  controls.enableDamping = true
  controls.minPolarAngle = -Math.PI / 2
  controls.maxPolarAngle = Math.PI / 2 - 0.1
  controls.minDistance = 1
  controls.maxDistance = 25
  controls.maxTargetRadius = 20
  controls.target = new THREE.Vector3(ROS_CONST / 2, ROS_CONST / 2, 0)

  //background color
  scene.background = new THREE.Color(0xffe699)

  const ambiantLight = new THREE.AmbientLight(0xe0e0e0)
  scene.add(ambiantLight)

  const light = new THREE.DirectionalLight(0xffffff, 1.6)
  light.position.set(12, -12, 30)
  light.target.position.set(0, 0, 0)
  light.castShadow = true
  light.shadow.bias = -0.00008
  light.shadow.mapSize.width = 1600
  light.shadow.mapSize.height = 1600
  light.shadow.camera.near = 0.1
  light.shadow.camera.far = 200
  light.shadow.camera.left = -25
  light.shadow.camera.right = 25
  light.shadow.camera.top = 25
  light.shadow.camera.bottom = -25
  light.shadow.radius = 1.8
  light.shadow.blurSamples = 10
  scene.add(light)

  // debug shadow camera
  // scene.add(new THREE.CameraHelper(light.shadow.camera))

  const plane = new THREE.Mesh(new THREE.PlaneGeometry(90, 90, 1, 1), new THREE.MeshLambertMaterial({ color: 0xffebab }))
  plane.receiveShadow = true
  plane.castShadow = false
  // plane.rotation.x = -Math.PI / 2
  scene.add(plane)

  loadPin(scene)
  click_handling(scene, camera, plane, viewWidth, viewHeight)

  const geometry = new THREE.BoxGeometry(1, 1, 1)

  const cube00 = new THREE.Mesh(geometry, new THREE.MeshLambertMaterial({ color: 0xff0000 }))

  cube00.position.set(0.0, 0.0, 0.5)
  cube00.scale.set(0.1, 0.1, 0.1)
  cube00.castShadow = true
  cube00.receiveShadow = true
  scene.add(cube00)

  const cube10 = new THREE.Mesh(geometry, new THREE.MeshLambertMaterial({ color: 0x0000ff }))

  cube10.position.set(1.0, 0.0, 0.5)
  cube10.scale.set(0.1, 0.1, 0.1)
  cube10.castShadow = true
  cube10.receiveShadow = true
  scene.add(cube10)

  const cube01 = new THREE.Mesh(geometry, new THREE.MeshLambertMaterial({ color: 0x6060af }))

  cube01.position.set(0.0, 1.0, 0.5)
  cube01.scale.set(0.1, 0.1, 0.1)
  cube01.castShadow = true
  cube01.receiveShadow = true
  scene.add(cube01)
}

export default defineComponent({
  name: 'ThreeWrapper',
  components: { NoWebGLDialog, Dialog, DialogDescription, DialogTitle, DialogPanel, XMarkIcon },
  data() {
    return {
      isWebGLAvailable: WebGL.isWebGLAvailable(),
      isGrabbing: false
    }
  },
  mounted() {
    if (!this.isWebGLAvailable) return

    initWorld()

    this.onResize()
    let threeAppDiv = document.getElementById('three-app')
    threeAppDiv.appendChild(renderer.domElement)
    threeAppDiv.appendChild(labelRenderer.domElement)

    addEventListener('resize', () => this.onResize())
    addEventListener('scroll', () => this.onResize())

    controls.addEventListener('start', () => this.isGrabbing = true)
    controls.addEventListener('end', () => this.isGrabbing = false)

    this.animate()

    this.webSocketsStuff()
  },
  unmounted() {
    if (!this.isWebGLAvailable) return
    removeEventListener('resize', () => this.onResize())
    resetRobots()
  },
  methods: {
    onResize() {
      let threeAppDiv = document.getElementById('three-app')
      let wrapper = document.getElementById('three-app-wrapper')

      top = 76
      let width = window.innerWidth
      let height = window.innerHeight - top
      viewWidth.value = width
      viewHeight.value = height

      renderer.setSize(width, height)
      labelRenderer.setSize(width, height)
      camera.aspect = width / height
      camera.updateProjectionMatrix()
      wrapper.style.height = `${height}px`
      threeAppDiv.style.top = `${top}px`
    },
    animate() {
      requestAnimationFrame(this.animate)

      // required if controls.enableDamping or controls.autoRotate are set to true
      controls.update()

      updateRobots(scene)
      animatePins()
      animateClouds()


      // FPS
      frames++
      const time = performance.now()
      if (time >= prevTime + 1000) {
        console.log('FPS:', Math.round((frames * 1000) / (time - prevTime)))
        frames = 0
        prevTime = time
      }
      //

      renderer.render(scene, camera)
      labelRenderer.render(scene, camera)
    },
    webSocketsStuff() {
      handleUpdateMapSockets(host, scene)

      handleRobotPosSocket(host, scene)

      handleUpdateGoalSocket(host, scene)
    }
  }
})
</script>

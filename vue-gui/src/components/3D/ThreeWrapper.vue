<template>
  <NoWebGLDialog :display="! isWebGLAvailable" />
  <div id="three-app-wrapper">
    <div id="three-app" class="absolute top-0 left-0" :class="isGrabbing ? 'cursor-grabbing' : 'cursor-grab'"></div>
  </div>
</template>

<script lang="js">
import { defineComponent } from 'vue'
import * as THREE from 'three'
import { Dialog, DialogDescription, DialogPanel, DialogTitle } from '@headlessui/vue'
import NoWebGLDialog from '@/components/util/NoWebGLDialog.vue'
import WebGL from 'three/addons/capabilities/WebGL.js'
import { MapControls } from 'three/addons/controls/MapControls.js'
import * as map_utils from './map_utils.ts'

var scene, camera, renderer, controls

/**
 * @type {map_utils.Point2D[]}
 */
const map2DPointsGroups = []

function initWorld() {
  // do not put these in the data() function because they will break if in a proxy
  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000)
  camera.position.set(0, 5, 5)
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.VSMShadowMap
  renderer.pixelRatio = window.devicePixelRatio

  // controls: see https://threejs.org/docs/#examples/en/controls/MapControls
  controls = new MapControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.minPolarAngle = 0
  controls.maxPolarAngle = Math.PI / 2 - 0.1
  controls.minDistance = 2
  controls.maxDistance = 25
  controls.maxTargetRadius = 20

  //background color
  scene.background = new THREE.Color(0xffe699)

  const ambiantLight = new THREE.AmbientLight(0xffffff)
  scene.add(ambiantLight)

  const light = new THREE.DirectionalLight(0xffffff, 1.6)
  light.position.set(12, 41, 12)
  light.target.position.set(0, 0, 0)
  light.castShadow = true
  light.shadow.bias = -0.0001
  light.shadow.mapSize.width = 768
  light.shadow.mapSize.height = 768
  light.shadow.camera.near = 0.1
  light.shadow.camera.far = 200
  light.shadow.camera.left = -25
  light.shadow.camera.right = 25
  light.shadow.camera.top = 25
  light.shadow.camera.bottom = -25
  light.shadow.radius = 2
  light.shadow.blurSamples = 6
  scene.add(light)

  // debug shadow camera
  // scene.add(new THREE.CameraHelper( light.shadow.camera ))

  const plane = new THREE.Mesh(new THREE.PlaneGeometry(50, 50, 1, 1), new THREE.MeshLambertMaterial({ color: 0xffebab }))
  plane.receiveShadow = true
  plane.castShadow = false
  plane.rotation.x = -Math.PI / 2
  scene.add(plane)
}

export default defineComponent({
  name: 'ThreeWrapper',
  components: { NoWebGLDialog, Dialog, DialogDescription, DialogTitle, DialogPanel },
  data() {
    return {
      isWebGLAvailable: WebGL.isWebGLAvailable(),
      isGrabbing: false
    }
  },
  mounted() {
    if (!this.isWebGLAvailable) return

    initWorld()

    let threeAppDiv = document.getElementById('three-app')
    this.onResize()
    threeAppDiv.appendChild(renderer.domElement)

    addEventListener('resize', () => this.onResize())

    controls.addEventListener('start', () => this.isGrabbing = true)
    controls.addEventListener('end', () => this.isGrabbing = false)

    this.animate()

    this.webSocketsStuff()
  },
  unmounted() {
    if (!this.isWebGLAvailable) return
    removeEventListener('resize', () => this.onResize())
  },
  methods: {
    onResize() {
      let threeAppDiv = document.getElementById('three-app')
      let wrapper = document.getElementById('three-app-wrapper')

      let divRect = wrapper.getBoundingClientRect()
      let width = window.innerWidth
      let height = window.innerHeight - divRect.y

      renderer.setSize(width, height)
      camera.aspect = width / height
      camera.updateProjectionMatrix()
      wrapper.style.height = `${height}px`
      threeAppDiv.style.top = `${divRect.y}px`
    },
    animate() {
      requestAnimationFrame(this.animate)

      // required if controls.enableDamping or controls.autoRotate are set to true
      controls.update()

      renderer.render(scene, camera)
    },
    webSocketsStuff() {
      const host = process.env.NODE_ENV === 'development' ? window.location.host.split(':')[0] : window.location.host
      console.log(`using host '${host}' as websocket host`)
      const mapSolidSocket = new WebSocket(`ws://${host}/ws/map/solid`)

      mapSolidSocket.addEventListener('open', () => {
        console.log('mapSolidSocket connected')
        mapSolidSocket.send('sendall')
      })

      mapSolidSocket.addEventListener('message', (event) => {
        console.log('mapSolidSocket message', event.data)

        map_utils.handleMapPointDiff(event.data, map2DPointsGroups, scene)
      })
    },
  }
})
</script>

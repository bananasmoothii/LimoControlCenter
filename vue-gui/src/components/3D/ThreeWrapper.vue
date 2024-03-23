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
import { handleUpdateMapSockets } from './map.ts'
import { handleRobotPosSocket, resetRobots, selectedRobot, updateRobots } from '@/components/3D/robots.ts'
import { CSS2DRenderer } from 'three/examples/jsm/renderers/CSS2DRenderer.js'
import { animatePin, loadPin, pinObj } from '@/components/3D/pin_goal.ts'

var scene, camera, renderer, labelRenderer, controls, width, height, top

function initWorld() {
  // do not put these in the data() function because they will break if in a proxy
  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000)
  camera.position.set(0, 4, 4)
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
  controls.minPolarAngle = 0
  controls.maxPolarAngle = Math.PI / 2 - 0.1
  controls.minDistance = 1
  controls.maxDistance = 25
  controls.maxTargetRadius = 20

  //background color
  scene.background = new THREE.Color(0xffe699)

  const ambiantLight = new THREE.AmbientLight(0xe0e0e0)
  scene.add(ambiantLight)

  const light = new THREE.DirectionalLight(0xffffff, 1.6)
  light.position.set(12, 30, 12)
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
  plane.rotation.x = -Math.PI / 2
  scene.add(plane)

  loadPin(scene)
  let raycaster = new THREE.Raycaster()
  let mouse = new THREE.Vector2()
  const minDistanceToConsiderAsDrag = 6
  let startX, startY
  document.addEventListener('mousedown', (event) => {
    startX = event.clientX
    startY = event.clientY
  })
  document.addEventListener('mouseup', (event) => {
    let distance = Math.sqrt(Math.pow(event.clientX - startX, 2) + Math.pow(event.clientY - startY, 2))
    if (distance < minDistanceToConsiderAsDrag && pinObj !== null) { // we need pinObj for almost all actions
      let scrolledWrapperTop = document.getElementById('three-app-wrapper').getBoundingClientRect().y
      mouse.x = (event.clientX / width) * 2 - 1
      mouse.y = -((event.clientY - scrolledWrapperTop) / height) * 2 + 1
      raycaster.setFromCamera(mouse, camera)

      let intersectRobot = raycaster.intersectObjects(scene.children.filter((obj) => obj.name.startsWith('robot-')))
      if (intersectRobot.length > 0) {
        let obj = intersectRobot[0].object
        while (!obj.name.startsWith('robot-')) {
          obj = obj.parent
        }
        console.log(obj.name)
        selectedRobot.value = obj.name.split('-', 2)[1]
        pinObj.visible = false
        return
      }

      selectedRobot.value = null

      if (pinObj.visible) {
        let intersectsPinGoal = raycaster.intersectObject(pinObj)
        if (intersectsPinGoal.length > 0) {
          pinObj.visible = false
          return
        }
      }

      let intersectPlane = raycaster.intersectObject(plane)
      if (intersectPlane.length > 0) {
        let intersect = intersectPlane[0]
        pinObj.visible = true
        pinObj.position.copy(intersect.point)
      } else {
        pinObj.visible = false
      }
    }
  })
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
      width = window.innerWidth
      height = window.innerHeight - top

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
      animatePin()

      renderer.render(scene, camera)
      labelRenderer.render(scene, camera)
    },
    webSocketsStuff() {
      const host = process.env.NODE_ENV === 'development' ? window.location.host.split(':')[0] : window.location.host
      console.log(`using host '${host}' as websocket host`)

      handleUpdateMapSockets(host, scene)

      handleRobotPosSocket(host, scene)
    }
  }
})
</script>

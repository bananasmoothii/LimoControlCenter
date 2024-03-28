import * as THREE from 'three'
import { InstancedMesh } from 'three'
import type { GLTF } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'

export const CUBE_SIZE: number = 0.05

export enum WallPointType {
  WALL = 'W',
  UNKNOWN = 'U',
  PASSABLE = 'P'
}

// extends Point2D
export type Point = {
  x: number
  y: number
  z?: number
  type: WallPointType,
}


export function handleUpdateMapSockets(host: string, scene: THREE.Scene) {
  const mapSolidSocket = new WebSocket(`ws://${host}/ws/update-map`)

  mapSolidSocket.addEventListener('open', () => {
    console.log('mapSolidSocket connected')
    mapSolidSocket.send('sendall')
  })

  mapSolidSocket.addEventListener('message', (event) => {
    // console.log('mapSolidSocket message', event.data)

    handleMapPointDiff(event.data, scene)
  })
}

const loader = new GLTFLoader()

let cloudsMesh: InstancedMesh[] = []

let cloudsLoaded = false
let loadingClouds = false

let cloudsMaterial = new THREE.MeshLambertMaterial({
  color: 0xf5f5f5,
  transparent: true,
  opacity: 0.6
})

function loadCloudsIfNeeded(scene: THREE.Object3D) {
  if (cloudsLoaded || loadingClouds) return
  loadingClouds = true
  for (let i = 1; i <= 4; i++) {
    loader.load(`/3D_models/clouds/cloud${i}.glb`, (gltf: GLTF) => {
      const cloud = gltf.scene.children[0] as THREE.Group
      let scale = 0.11
      cloud.traverse((child) => {
        if ((child as THREE.Mesh).isMesh) {
          let geometry = (child as THREE.Mesh).geometry
          geometry.scale(scale, scale, scale)
          const instanceMesh = new InstancedMesh(geometry, cloudsMaterial, 3000)
          instanceMesh.count = 0
          instanceMesh.castShadow = true
          instanceMesh.receiveShadow = true
          instanceMesh.frustumCulled = false
          cloudsMesh.push(instanceMesh)
          cloudPositions.push({ x: [], y: [] })
          scene.add(instanceMesh)
        }
      })
      if (cloudsMesh.length === 4) {
        cloudsLoaded = true
        loadingClouds = false
        executeOnCloudsLoaded.forEach(f => f())
      }
    })
  }
}

const executeOnCloudsLoaded: (() => void)[] = []

export function doWithCloudsLoaded(callback: () => void) {
  if (cloudsLoaded) {
    callback()
  } else {
    executeOnCloudsLoaded.push(callback)
  }
}

const smallWall = new THREE.BoxGeometry(CUBE_SIZE, CUBE_SIZE, CUBE_SIZE * 2)
smallWall.translate(0, 0, CUBE_SIZE)
const wall = new THREE.BoxGeometry(CUBE_SIZE, CUBE_SIZE, CUBE_SIZE * 8)
wall.translate(0, 0, CUBE_SIZE * 4)

const wallMaterial = new THREE.MeshLambertMaterial({
  color: 0xffffff
})
const unknownMaterial = new THREE.MeshLambertMaterial({
  color: 0x888888
})

// 50 is a default value, if more are needed, you need to create a new mesh

let wallMesh: InstancedMesh = new InstancedMesh(wall, wallMaterial, 3000)
wallMesh.count = 0

// let unknownMesh: InstancedMesh = new InstancedMesh(smallWall, unknownMaterial, 60000)
// unknownMesh.count = 0

wallMesh.castShadow = true
wallMesh.receiveShadow = true
wallMesh.frustumCulled = false
// unknownMesh.castShadow = true
// unknownMesh.receiveShadow = true
// unknownMesh.frustumCulled = false

let meshAdded = false

const dummy = new THREE.Object3D()

let wallPositionsX: number[] = []
let wallPositionsY: number[] = []

let unknownPositionsX: number[] = []
let unknownPositionsY: number[] = []

let cloudPositions: { x: number[], y: number[] }[] = []

function forAllCloudPos(callback: (x: number, y: number) => boolean | void) {
  for (let cloud of cloudPositions) {
    for (let i = 0; i < cloud.x.length; i++) {
      if (callback(cloud.x[i], cloud.y[i])) return
    }
  }
}

function searchPointIndex(x: number, y: number, positionsX: number[], positionsY: number[]): number {
  for (let i = 0; i < positionsX.length; i++) {
    if (positionsX[i] === x && positionsY[i] === y) return i
  }
  return -1

}

function removeWallAtIndex(index: number, positionsX: number[], positionsY: number[], mesh: InstancedMesh) {
  let x = positionsX.pop()!
  let y = positionsY.pop()!
  mesh.getMatrixAt(mesh.count, dummy.matrix)
  dummy.position.set(x, y, 0)
  dummy.rotation.set(0, 0, 0)
  dummy.updateMatrix()
  mesh.setMatrixAt(index, dummy.matrix)
  positionsX[index] = x
  positionsY[index] = y
  mesh.count--
  mesh.instanceMatrix.needsUpdate = true
}

function removePointsAt(point: Point) {
  const wallIndex = searchPointIndex(point.x, point.y, wallPositionsX, wallPositionsY)
  if (wallIndex !== -1) {
    removeWallAtIndex(wallIndex, wallPositionsX, wallPositionsY, wallMesh)
  }
  const unknownIndex = searchPointIndex(point.x, point.y, unknownPositionsX, unknownPositionsY)
  if (unknownIndex !== -1) {
    // removeWallAtIndex(unknownIndex, unknownPositionsX, unknownPositionsY, unknownMesh)

    for (let i = 0; i < cloudPositions.length; i++) {
      const cloudIndex = searchPointIndex(point.x, point.y, cloudPositions[i].x, cloudPositions[i].y)
      if (cloudIndex !== -1) {
        removeWallAtIndex(cloudIndex, cloudPositions[i].x, cloudPositions[i].y, cloudsMesh[i])
      }
    }
  }
}

export function handleMapPointDiff(diff: string, scene: THREE.Object3D) {
  loadCloudsIfNeeded(scene)

  if (!meshAdded) {

    scene.add(wallMesh)
    // scene.add(unknownMesh)
    meshAdded = true
  }

  deserializeMapPointsDiffForEach(diff, function callback(point) {
    if (point.z !== undefined) {
      console.warn('3D map does not support 3D points yet')
      return
    }

    // remove existing things at this pos
    removePointsAt(point)

    if (point.type === WallPointType.PASSABLE) return // passable is represented by absence of obstacles

    if (point.type === WallPointType.WALL) {
      addPoint(point, wallMesh, wallPositionsX, wallPositionsY, (newMesh) => {
        wallMesh.removeFromParent()
        wallMesh.dispose()
        wallMesh = newMesh
        scene.add(wallMesh)
      })
    } else {
      // addPoint(point, unknownMesh, unknownPositionsX, unknownPositionsY, (newMesh) => {
      //   unknownMesh.removeFromParent()
      //   unknownMesh.dispose()
      //   unknownMesh = newMesh
      //   scene.add(unknownMesh)
      // })
      doWithCloudsLoaded(() => {
        let x = point.x
        let y = point.y
        let isNearOtherCloud = false
        forAllCloudPos((cloudX, cloudY) => {
          if (Math.abs(cloudX - x) < CUBE_SIZE * 3 && Math.abs(cloudY - y) < CUBE_SIZE * 3) {
            isNearOtherCloud = true
            return true
          }
        })
        // let xForRandom = Math.round(point.x / CUBE_SIZE)
        // let yForRandom = Math.round(point.y / CUBE_SIZE)
        if (/*xForRandom % 3 !== 0 || yForRandom % 3 !== 0*/ isNearOtherCloud || Math.floor(Math.random() * 1.2) === 0) return
        let cloudNb = Math.floor(Math.random() * 4)
        addPoint(point, cloudsMesh[cloudNb], cloudPositions[cloudNb].x, cloudPositions[cloudNb].y, (newMesh) => {
          cloudsMesh[cloudNb].removeFromParent()
          cloudsMesh[cloudNb].dispose()
          cloudsMesh[cloudNb] = newMesh
          scene.add(cloudsMesh[cloudNb])
        })
      })
    }
  })
}

function addPoint(
  point: Point,
  mesh: InstancedMesh,
  positionsX: number[],
  positionsY: number[],
  setMesh: (mesh: InstancedMesh) => void
) {
  let mesh2 = mesh
  let index = findFreePos(mesh, newMesh => {
    mesh2 = newMesh
    setMesh(newMesh)
  })
  // console.log("index", index)
  positionsX[index] = point.x
  positionsY[index] = point.y
  dummy.position.set(point.x, point.y, 0)
  dummy.rotation.set(0, 0, 0)
  dummy.updateMatrix()
  mesh2.setMatrixAt(mesh2.count, dummy.matrix)
  mesh2.count++
  mesh2.instanceMatrix.needsUpdate = true
}

// see https://stackoverflow.com/questions/1100311/what-is-the-ideal-growth-rate-for-a-dynamically-allocated-array
const GROWTH_FACTOR = 1.5

function findFreePos(mesh: InstancedMesh, setMesh: (mesh: InstancedMesh) => void): number {
  if (mesh.count >= mesh.instanceMatrix.count) {
    let newSize = Math.ceil(mesh.instanceMatrix.count * GROWTH_FACTOR)
    console.log(`growing mesh from ${mesh.instanceMatrix.count} (${mesh.count}) to ${newSize}`)
    const newMesh = new InstancedMesh(mesh.geometry, mesh.material, newSize)
    newMesh.count = mesh.count
    for (let i = 0; i < mesh.instanceMatrix.count; i++) {
      mesh.getMatrixAt(i, dummy.matrix)
      newMesh.setMatrixAt(i, dummy.matrix)
    }
    // newMesh.copy(mesh)
    console.log(`new mesh size: ${newMesh.instanceMatrix.count} (${newMesh.count})`)
    setMesh(newMesh)
    return newMesh.count
  }
  return mesh.count
}

function deserializeSinglePoint(point: string): Point {
  const type = point[0] as WallPointType
  const splitted = point.substring(1).split(',')
  const x = parseFloat(splitted[0])
  const y = parseFloat(splitted[1])
  const z = splitted.length === 3 ? parseFloat(splitted[2]) : undefined
  return { x, y, z, type }
}

const onlySpacesRegex = /^\s*$/

/**
 * Deserialize a map points difference to a list of string points to add and remove
 * format of the serialized string: "Tx,y[,z] Tx,y[,z] " where T is the type of the point (W, U, P)
 * For example: "W0,0 W0,1 W0,2 U0,3 " will add 3 wall points and 1 unknown point
 * By default, all points are unknown
 */
export function deserializeMapPointsDiff(serialized: string): Point[] {
  const points: Point[] = []
  deserializeMapPointsDiffForEach(serialized, (point) => points.push(point))
  return points
}

export function deserializeMapPointsDiffForEach(serialized: string, callback: (point: Point) => void) {
  if (onlySpacesRegex.test(serialized)) return []
  const points = serialized.split(' ').sort()
  // points.forEach(point => {
  //   if (point.length < 2) {
  //     console.warn(`Invalid point: ${point}`)
  //     return
  //   }
  //   callback(deserializeSinglePoint(point))
  // })

  let pointsLength = points.length
  let i = 0
  while (i < pointsLength) {
    const point = points[i]
    if (point.length < 2) {
      console.warn(`Invalid point: ${point}`)
      i++
      continue
    }
    callback(deserializeSinglePoint(point))
    i++
  }

  // let point = ''
  // for (let i = 0; i < serialized.length; i++) {
  //   if (serialized[i] === ' ') {
  //     if (point.length > 0) {
  //       callback(deserializeSinglePoint(point))
  //       point = ''
  //     }
  //   } else {
  //     point += serialized[i]
  //   }
  // }
}

export function serializeStringPoint(point: Point): string {
  if (point.z !== undefined) return `${point.x},${point.y},${point.z}`
  return `${point.x},${point.y}`
}

export function animateClouds() {
  for (let cloudMesh of cloudsMesh) {
    for (let i = 0; i < cloudMesh.count; i++) {
      cloudMesh.getMatrixAt(i, dummy.matrix)
      dummy.position.setFromMatrixPosition(dummy.matrix)
      dummy.position.z = CUBE_SIZE * 8 + 0.08 * Math.sin(dummy.position.x + dummy.position.y + Date.now() / 2000)
      dummy.rotation.x = Math.PI / 2
      dummy.rotation.y = (dummy.position.x + dummy.position.y) * 100 + Date.now() / 8000
      dummy.updateMatrix()
      cloudMesh.setMatrixAt(i, dummy.matrix)
    }
    cloudMesh.instanceMatrix.needsUpdate = true
  }
}

import * as THREE from 'three'
import { InstancedMesh } from 'three'

export const CUBE_SIZE: number = 0.0457

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


// const cube = new THREE.BoxGeometry(CUBE_SIZE, CUBE_SIZE, CUBE_SIZE)
const wall = new THREE.BoxGeometry(CUBE_SIZE, CUBE_SIZE * 8, CUBE_SIZE)

const wallMaterial = new THREE.MeshLambertMaterial({
  color: 0xffffff
})
const unknownMaterial = new THREE.MeshLambertMaterial({
  color: 0x444444
})

// 50 is a default value, if more are needed, you need to create a new mesh

let wallMesh: InstancedMesh = new InstancedMesh(wall, wallMaterial, 3000)
wallMesh.count = 0

let unknownMesh: InstancedMesh = new InstancedMesh(wall, unknownMaterial, 3000)
unknownMesh.count = 0

wallMesh.castShadow = true
wallMesh.receiveShadow = true
unknownMesh.castShadow = true
unknownMesh.receiveShadow = true

let meshAdded = false

const dummy = new THREE.Object3D()

let wallPositionsX: number[] = []
let wallPositionsY: number[] = []

let unknownPositionsX: number[] = []
let unknownPositionsY: number[] = []

function getPointIndex(x: number, y: number, positionsX: number[], positionsY: number[]): number {
  for (let i = 0; i < positionsX.length; i++) {
    if (positionsX[i] === x && positionsY[i] === y) return i
  }
  return -1

}

export function handleMapPointDiff(diff: string, scene: THREE.Object3D) {
  const changedPoints = deserializeMapPointsDiff(diff)

  if (!meshAdded) {
    scene.add(wallMesh)
    scene.add(unknownMesh)
    meshAdded = true
  }

  for (const point of changedPoints) {
    if (point.z !== undefined) {
      console.warn('3D map does not support 3D points yet')
      continue
    }

    // remove existing things at this pos
    const wallIndex = getPointIndex(point.x, point.y, wallPositionsX, wallPositionsY)
    if (wallIndex !== -1) {
      wallPositionsX.splice(wallIndex, 1)
      wallPositionsY.splice(wallIndex, 1)
      wallMesh.getMatrixAt(wallMesh.count - 1, dummy.matrix)
      wallMesh.setMatrixAt(wallIndex, dummy.matrix)
      wallMesh.count--
    }
    const unknownIndex = getPointIndex(point.x, point.y, unknownPositionsX, unknownPositionsY)
    if (unknownIndex !== -1) {
      unknownPositionsX.splice(unknownIndex, 1)
      unknownPositionsY.splice(unknownIndex, 1)
      unknownMesh.getMatrixAt(unknownMesh.count - 1, dummy.matrix)
      unknownMesh.setMatrixAt(unknownIndex, dummy.matrix)
      unknownMesh.count--
    }

    if (point.type === WallPointType.PASSABLE) continue // passable is represented by absence of obstacles

    if (point.type === WallPointType.WALL) {
      addPoint(point, wallMesh, wallPositionsX, wallPositionsY, (newMesh) => {
        wallMesh.removeFromParent()
        wallMesh.dispose()
        wallMesh = newMesh
        scene.add(wallMesh)
      })
    } else {
      addPoint(point, unknownMesh, unknownPositionsX, unknownPositionsY, (newMesh) => {
        unknownMesh.removeFromParent()
        unknownMesh.dispose()
        unknownMesh = newMesh
        scene.add(unknownMesh)
      })
    }
  }
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
  dummy.position.set(point.x, 0, point.y)
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
  if (onlySpacesRegex.test(serialized)) return []
  const points = serialized.split(' ')
  const deserializedPoints: Point[] = []
  for (const point of points) {
    if (point.length < 3) {
      console.warn(`Invalid point: ${point}`)
      continue
    }
    deserializedPoints.push(deserializeSinglePoint(point))
  }
  return deserializedPoints
}

export function serializeStringPoint(point: Point): string {
  if (point.z !== undefined) return `${point.x},${point.y},${point.z}`
  return `${point.x},${point.y}`
}

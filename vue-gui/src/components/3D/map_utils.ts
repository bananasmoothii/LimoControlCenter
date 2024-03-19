import * as THREE from 'three'
import * as BufferGeometryUtils from 'three/addons/utils/BufferGeometryUtils.js'

export const CUBE_SIZE: number = 0.2

export type Point2D = {
  x: number
  y: number
}

// extends Point2D
export type Point = {
  x: number
  y: number
  z?: number
}

export type Point2DGroups = {
  [key: string]: Point2D[]
}

export function is2DPoint(point: Point): point is Point2D {
  return point.z === undefined
}

export function separate2DAnd3DPoints(points: Point[]): { D2: Point2D[], D3: Point[] } {
  const D2: Point2D[] = []
  const D3: Point[] = []
  for (const point of points) {
    if (is2DPoint(point)) {
      D2.push(point)
    } else {
      D3.push(point)
    }
  }
  return { D2, D3 }
}

export function handleMapPointDiff(diff: string, map2DPointsGroups: Point2DGroups, scene: THREE.Scene) {
  const { add, remove } = deserializeMapPointsDiff(diff)

  const separateAdd = separate2DAnd3DPoints(add || [])
  const separateAdd2D = separateAdd.D2
  const separateAdd3D = separateAdd.D3

  const separateRemove = separate2DAnd3DPoints(remove || [])
  const separateRemove2D = separateRemove.D2
  const separateRemove3D = separateRemove.D3

  // just bulk remove and add 3D points, we don't care about these
  for (const remove3D of separateRemove3D) {
    scene.getObjectByName(serializeStringPoint(remove3D))?.removeFromParent()
  }
  const cube = new THREE.BoxGeometry(CUBE_SIZE, CUBE_SIZE, CUBE_SIZE)
  const material = new THREE.MeshLambertMaterial({ color: 0xffffff })
  for (const add3D of separateAdd3D) {
    const mesh = new THREE.Mesh(cube, material)
    mesh.castShadow = true
    mesh.receiveShadow = true
    mesh.position.set(add3D.x, add3D.z!!, add3D.y)
    mesh.name = serializeStringPoint(add3D)
    scene.add(mesh)
  }

  // 2D points
  const groupModifications: GroupModifications = { changedGroups: new Set(), removedGroups: new Set() }
  if (separateAdd2D || separateRemove2D) { // do nothing if remove is empty / null
    for (const remove2D of separateRemove2D) {
      removePointFromGroupOfConnectedPoints(remove2D, map2DPointsGroups, groupModifications)
    }
    for (const add2D of separateAdd2D) {
      addPointToGroupOfConnectedPoints(add2D, map2DPointsGroups, groupModifications)
    }

    for (const groupId of groupModifications.removedGroups) {
      scene.getObjectByName(groupId)?.removeFromParent()
    }
    for (const groupId of groupModifications.changedGroups) {
      scene.getObjectByName(groupId)?.removeFromParent()
      const points = map2DPointsGroups[groupId]
      const mesh = mapPointGroupToMesh(points)
      mesh.name = groupId
      scene.add(mesh)
    }
  }
}

/**
 * Deserialize a map points difference to a list of string points to add and remove
 */
export function deserializeMapPointsDiff(serialized: string): { add: Point[] | null, remove: Point[] | null } {
  const [addStr, removeStr] = serialized.split('/', 2)
  const add = addStr ? addStr.split(' ') : null
  if (add && add[add.length - 1] === '') add.pop()
  const remove = removeStr ? removeStr.split(' ') : null
  if (remove && remove[remove.length - 1] === '') remove.pop()

  return {
    add: add && add.map(deserializeStringPoint),
    remove: remove && remove.map(deserializeStringPoint)
  }
}

export function serializeStringPoint(point: Point): string {
  if (point.z !== undefined) return `${point.x},${point.y},${point.z}`
  return `${point.x},${point.y}`
}

export function deserializeStringPoint(serialized: string): Point {
  const splitted = serialized.split(',')
  return {
    x: parseFloat(splitted[0]),
    y: parseFloat(splitted[1]),
    z: splitted.length === 3 ? parseFloat(splitted[2]) : undefined
  }
}

// export function pointToGeometry(point: Point) {
//   let pointIs2D = point.z === undefined
//
//   const cube = new THREE.BoxGeometry(CUBE_SIZE, pointIs2D ? 1.2 : CUBE_SIZE, CUBE_SIZE)
//   // Warning: the y and z are swapped because of the coordinate system
//   cube.translate(point.x, pointIs2D ? 0.6 : point.z!!, point.y)
//   return cube
// }

// export function addPointToGroupOfConnectedPoints(currentPoint: Point2D, connectedPoints: Point2D[][]) {
//   let pointAdded = false
//   for (const group of connectedPoints) {
//     for (const point of group) {
//       if (Math.abs(currentPoint.x - point.x) <= CUBE_SIZE && Math.abs(currentPoint.y - point.y) <= CUBE_SIZE) {
//         group.push(currentPoint)
//         pointAdded = true
//       }
//     }
//   }
//   if (!pointAdded) connectedPoints.push([currentPoint])
// }

export function removeAll<T>(array: Array<T>, predicate: (value: T, index: number, obj: T[]) => boolean) {
  let i = array.length
  while (i--) {
    if (predicate(array[i], i, array)) {
      array.splice(i, 1)
    }
  }
}

type GroupModifications = { changedGroups: Set<string>; removedGroups: Set<string> }

function removePointFromGroupOfConnectedPoints(
  currentPoint: Point2D,
  connectedPoints: Point2DGroups,
  groupModifications: GroupModifications
) {
  for (const [groupId, points] of Object.entries(connectedPoints)) {
    let hasGroupChanged = false
    removeAll(points, (point) => {
      const hasSameCoords = point.x === currentPoint.x && point.y === currentPoint.y
      if (hasSameCoords) hasGroupChanged = true
      return hasSameCoords
    })
    if (hasGroupChanged) {
      if (points.length === 0) groupModifications.removedGroups.add(groupId)
      else groupModifications.changedGroups.add(groupId)
    }
  }
}

function addPointToGroupOfConnectedPoints(
  currentPoint: Point2D,
  connectedPoints: Point2DGroups,
  groupModifications: GroupModifications
) {
  for (const [groupId, points] of Object.entries(connectedPoints)) {
    for (const point of points) {
      if (Math.abs(currentPoint.x - point.x) <= CUBE_SIZE + 1e-4 && Math.abs(currentPoint.y - point.y) <= CUBE_SIZE + 1e-4) {
        points.push(currentPoint)
        groupModifications.changedGroups.add(groupId)

        return
      }
    }
  }
  // point not found in any group, create a new group
  const groupId = `2d point group ${currentPoint.x},${currentPoint.y}`
  connectedPoints[groupId] = [currentPoint]
  groupModifications.changedGroups.add(groupId)
}

function mapPointGroupToMesh(points: Point2D[]): THREE.Mesh {
  const material = new THREE.MeshLambertMaterial({ color: Math.random() * 16777215 })
  const geometries = points.map((point) => {
    const cube = new THREE.BoxGeometry(CUBE_SIZE, 1.2, CUBE_SIZE)
    cube.translate(point.x, 0.6, point.y)
    return cube
  })
  const geometry = BufferGeometryUtils.mergeGeometries(geometries)
  const mesh = new THREE.Mesh(geometry, material)
  mesh.castShadow = true
  mesh.receiveShadow = true
  return mesh
}
import * as THREE from 'three'

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
    console.log('mapSolidSocket message', event.data)

    handleMapPointDiff(event.data, scene)
  })
}

export function handleMapPointDiff(diff: string, scene: THREE.Scene) {
  const changedPoints = deserializeMapPointsDiff(diff)

  const cube = new THREE.BoxGeometry(CUBE_SIZE, CUBE_SIZE, CUBE_SIZE)
  const wall = new THREE.BoxGeometry(CUBE_SIZE, CUBE_SIZE * 8, CUBE_SIZE)
  const wallMaterial = new THREE.MeshLambertMaterial({ color: 0xffffff })
  const unknownMaterial = new THREE.MeshLambertMaterial({ color: 0x444444 })
  for (const point of changedPoints) {
    // remove existing things at this pos
    for (const type in WallPointType) {
      const possibleObjectName = serializeStringPoint({ ...point, type: type as WallPointType })
      scene.getObjectByName(possibleObjectName)?.removeFromParent()
    }

    if (point.type === WallPointType.PASSABLE) continue // passable is represented by absence of obstacles

    const mesh = new THREE.Mesh(
      point.z === undefined ? wall : cube,
      point.type === WallPointType.WALL ? wallMaterial : unknownMaterial
    )
    mesh.castShadow = true
    mesh.receiveShadow = true
    mesh.position.set(point.x, point.z ?? CUBE_SIZE * 3, point.y)
    mesh.name = serializeStringPoint(point)
    scene.add(mesh)

  }
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

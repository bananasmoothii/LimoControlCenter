import * as THREE from 'three'
import { Material } from 'three'

export function cloneMeshMaterial(mesh: THREE.Mesh) {
  if (mesh.material instanceof Material) {
    mesh.material = mesh.material.clone()
  } else if (mesh.material instanceof Array) {
    mesh.material = mesh.material.map(m => m.clone())
  }
}

// @ts-ignore
export const host = process.env.NODE_ENV === 'development' ? window.location.host.split(':')[0] : window.location.host
console.log(`using host '${host}' as host`)
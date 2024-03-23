import * as THREE from 'three'
import { Material } from 'three'

export function cloneMeshMaterial(mesh: THREE.Mesh) {
  if (mesh.material instanceof Material) {
    mesh.material = mesh.material.clone()
  } else if (mesh.material instanceof Array) {
    mesh.material = mesh.material.map(m => m.clone())
  }
}
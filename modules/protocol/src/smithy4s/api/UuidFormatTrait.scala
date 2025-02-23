/*
 *  Copyright 2021 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.api

import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.AnnotationTrait

case class UuidFormatTrait()
    extends AnnotationTrait(
      UuidFormatTrait.ID,
      UuidFormatTrait.toNode()
    ) {}

object UuidFormatTrait {

  private def toNode(): ObjectNode = Node.objectNode()
  private def fromNode(node: Node): UuidFormatTrait = UuidFormatTrait()

  val ID: ShapeId = ShapeId.from("smithy4s.api#uuidFormat")

  final class Provider
      extends AnnotationTrait.Provider[UuidFormatTrait](ID, fromNode) {}

}

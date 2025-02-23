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

package smithy4s.codegen

import cats.syntax.all._
import cats.~>
import smithy4s.codegen.Hint.Native
import smithy4s.codegen.Type.Alias
import smithy4s.codegen.Type.PrimitiveType
import smithy4s.codegen.TypedNode._

object CollisionAvoidance {
  def apply(compilationUnit: CompilationUnit): CompilationUnit = {

    val declarations = compilationUnit.declarations.map {
      case Service(name, originalName, ops, hints, version) =>
        val newOps = ops.map {
          case Operation(
                name,
                ns,
                params,
                input,
                errors,
                output,
                streamedInput,
                streamedOutput,
                hints
              ) =>
            Operation(
              protect(name.capitalize),
              ns,
              params.map(modField),
              modType(input),
              errors.map(modType),
              modType(output),
              streamedInput.map(modStreamingField),
              streamedOutput.map(modStreamingField),
              hints.map(modHint)
            )
        }
        Service(
          protect(name.capitalize),
          originalName,
          newOps,
          hints.map(modHint),
          version
        )
      case Product(name, fields, recursive, hints) =>
        Product(
          protect(name.capitalize),
          fields.map(modField),
          recursive,
          hints.map(modHint)
        )
      case Union(name, alts, recursive, hints) =>
        Union(
          protect(name.capitalize),
          alts.map(modAlt),
          recursive,
          hints.map(modHint)
        )
      case TypeAlias(name, tpe, hints) =>
        TypeAlias(protect(name.capitalize), modType(tpe), hints.map(modHint))
      case Enumeration(name, values, hints) =>
        val newValues = values.map { case EnumValue(value, name, hints) =>
          EnumValue(value, name.map(protect), hints.map(modHint))
        }
        Enumeration(protect(name.capitalize), newValues, hints.map(modHint))
    }
    CompilationUnit(compilationUnit.namespace, declarations)
  }

  private def modType(tpe: Type): Type = tpe match {
    case Type.List(member)         => Type.List(modType(member))
    case Type.Set(member)          => Type.Set(modType(member))
    case Type.Map(key, value)      => Type.Map(modType(key), modType(value))
    case Type.Ref(namespace, name) => Type.Ref(namespace, name.capitalize)
    case Alias(namespace, name, tpe) =>
      Alias(namespace, protect(name.capitalize), modType(tpe))
    case PrimitiveType(prim) => PrimitiveType(prim)
  }

  private def modField(field: Field): Field = {
    Field(
      protect(uncapitalise(field.name)),
      field.name,
      modType(field.tpe),
      field.required,
      field.hints.map(modHint)
    )
  }

  private def modStreamingField(
      streamingField: StreamingField
  ): StreamingField = {
    StreamingField(
      streamingField.name,
      modType(streamingField.tpe),
      streamingField.hints.map(modHint)
    )
  }

  private def modAlt(alt: Alt): Alt = {
    Alt(
      protect(uncapitalise(alt.name)),
      alt.name,
      modType(alt.tpe),
      alt.hints.map(modHint)
    )
  }

  private def modHint(hint: Hint): Hint = hint match {
    case Native(nt) => Native(smithy4s.recursion.preprocess(modTypedNode)(nt))
    case other      => other
  }

  private def modTypedNode: TypedNode ~> TypedNode =
    new (TypedNode ~> TypedNode) {
      def modRef(ref: Type.Ref): Type.Ref =
        Type.Ref(ref.namespace, ref.name.capitalize)

      def apply[A](fa: TypedNode[A]): TypedNode[A] = fa match {
        case EnumerationTN(ref, value, name) =>
          EnumerationTN(modRef(ref), value, name)
        case StructureTN(ref, fields) =>
          StructureTN(modRef(ref), fields)
        case NewTypeTN(ref, target) =>
          NewTypeTN(modRef(ref), target)
        case AltTN(ref, altName, alt) =>
          AltTN(modRef(ref), altName, alt)
        case MapTN(values) =>
          MapTN(values)
        case ListTN(values) =>
          ListTN(values)
        case SetTN(values) =>
          SetTN(values)
        case PrimitiveTN(prim, value) =>
          PrimitiveTN(prim, value)
      }
    }

  private def protect(str: String) =
    if (reservedNames(str)) s"_${str}" else str

  private val reservedKeywords: Set[String] = Set(
    "abstract",
    "case",
    "catch",
    "class",
    "def",
    "do",
    "else",
    "extends",
    "false",
    "final",
    "finally",
    "for",
    "forSome",
    "if",
    "implicit",
    "import",
    "lazy",
    "match",
    "new",
    "null",
    "object",
    "override",
    "package",
    "private",
    "protected",
    "return",
    "sealed",
    "super",
    "this",
    "throw",
    "trait",
    "true",
    "try",
    "type",
    "val",
    "var",
    "while",
    "with",
    "yield"
  )

  private val reservedTypes: Set[String] = Set(
    "String",
    "Byte",
    "Bytes",
    "Int",
    "Short",
    "Boolean",
    "Long",
    "Double",
    "Float",
    "BigInt",
    "BigDecimal",
    "Map",
    "List",
    "Set"
  )

  private val reservedNames = reservedKeywords ++ reservedTypes

  class Names(compilationUnit: CompilationUnit) {

    // TODO : implement better avoidance
    val definitions = compilationUnit.declarations.foldMap { d => Set(d.name) }

    val Transformation_ = "smithy4s.Transformation"
    val Service_ = "smithy4s.Service"
    val Endpoint_ = "smithy4s.Endpoint"
    val NoInput_ = "smithy4s.NoInput"
    val ShapeId_ = "smithy4s.ShapeId"
    val Schema_ = "smithy4s.Schema"
    val StreamingSchema_ = "smithy4s.StreamingSchema"
    val Enumeration_ = "smithy4s.Enumeration"
    val Hints_ = "smithy4s.Hints"
    val Static_ = "schematic.Static"
    val Errorable_ = "smithy4s.Errorable"
    val errorUnion_ = "smithy4s.errorUnion"

    def reconcile(str: String): String = {
      val last = str.split('.').last
      if (definitions.contains(last)) str else last
    }

  }

}

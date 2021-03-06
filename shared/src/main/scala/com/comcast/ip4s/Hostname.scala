/*
 * Copyright 2018 Comcast Cable Communications Management, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comcast.ip4s

import scala.util.hashing.MurmurHash3

import cats.{Order, Show}

/**
  * RFC1123 compliant hostname.
  *
  * A hostname contains one or more labels, where each label consists of letters A-Z, a-z, digits 0-9, or a dash.
  * A label may not start or end in a dash and may not exceed 63 characters in length. Labels are separated by
  * periods and the overall hostname must not exceed 253 characters in length.
  */
final class Hostname private (val labels: List[Hostname.Label], override val toString: String)
    extends HostnamePlatform
    with Ordered[Hostname] {

  /** Converts this hostname to lower case. */
  def normalized: Hostname =
    new Hostname(labels.map(l => new Hostname.Label(l.toString.toLowerCase)), toString.toLowerCase)

  def compare(that: Hostname): Int = toString.compare(that.toString)
  override def hashCode: Int = MurmurHash3.stringHash(toString, "Hostname".hashCode)
  override def equals(other: Any): Boolean =
    other match {
      case that: Hostname => toString == that.toString
      case _              => false
    }
}

object Hostname {

  /**
    * Label component of a hostname.
    *
    * A label consists of letters A-Z, a-z, digits 0-9, or a dash. A label may not start or end in a
    * dash and may not exceed 63 characters in length.
    */
  final class Label private[Hostname] (override val toString: String) extends Serializable with Ordered[Label] {
    def compare(that: Label): Int = toString.compare(that.toString)
    override def hashCode: Int = MurmurHash3.stringHash(toString, "Label".hashCode)
    override def equals(other: Any): Boolean =
      other match {
        case that: Label => toString == that.toString
        case _           => false
      }
  }

  private val Pattern =
    """[a-zA-Z0-9](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?)*""".r

  /** Constructs a `Hostname` from a string. */
  def apply(value: String): Option[Hostname] =
    value.size match {
      case 0            => None
      case i if i > 253 => None
      case _ =>
        value match {
          case Pattern(_*) =>
            val labels = value
              .split('.')
              .iterator
              .map(new Label(_))
              .toList
            if (labels.isEmpty) None else Option(new Hostname(labels, value))
          case _ => None
        }
    }

  implicit val order: Order[Hostname] = Order.fromComparable[Hostname]
  implicit val show: Show[Hostname] = Show.fromToString[Hostname]
}

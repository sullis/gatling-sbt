/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.sbt.utils

import java.util.jar.JarFile

import scala.collection.JavaConversions._

import sbt._

object CopyUtils {

  def extractFromCoreJar[T](updateReport: UpdateReport, fileName: String)(f: File => T) =
    extractFromJar(updateReport, "gatling-core", fileName)(f)

  def extractFromRecorderJar[T](updateReport: UpdateReport, fileName: String)(f: File => T) =
    extractFromJar(updateReport, "gatling-recorder", fileName)(f)

  private def extractFromJar[T](updateReport: UpdateReport, jarName: String, fileName: String)(f: File => T) = {
    val jar = getJarFromUpdateReport(updateReport, jarName)
    withFileInJar(jar, fileName)(f)
  }

  private def getJarFromUpdateReport(updateReport: UpdateReport, name: String) =
    updateReport.select(artifact = artifactFilter(new ExactFilter(name))).headOption match {
      case Some(jar) => jar
      case None =>
        throw new IllegalStateException(s"Could not find $name jar in dependencies. Please add it to your dependencies.")
    }

  private def withFileInJar[T](jarPath: File, fileName: String)(f: File => T) = {
    val jarFile = new JarFile(jarPath)
    val possibleEntry = jarFile.entries().find(_.getName.endsWith(fileName))
    possibleEntry match {
      case Some(entry) =>
        IO.withTemporaryFile("copy", "sbttemp") { file =>
          IO.write(file, IO.readBytes(jarFile.getInputStream(entry)))
          f(file)
        }
      case None =>
        throw new IllegalArgumentException(s"Could not find entry with fileName $fileName in jar $jarPath")
    }
  }
}

/*
 * Copyright (c) bdew, 2014 - 2020
 * https://github.com/bdew/ae2stuff
 *
 * This mod is distributed under the terms of the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package net.bdew.ae2stuff

import appeng.api.definitions.IItemDefinition
import appeng.api.util.{AEColor, AEColoredItemDefinition}
import net.bdew.lib.recipes.gencfg.{ConfigSection, GenericConfigLoader, GenericConfigParser}
import net.bdew.lib.recipes.{RecipeLoader, RecipeParser, RecipesHelper, StackRef}
import net.minecraft.item.ItemStack

import java.io.File
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.util.Optional

object Tuning extends ConfigSection

object TuningLoader {

  case class StackMaterial(name: String) extends StackRef

  case class StackPart(name: String) extends StackRef

  case class StackPartColored(name: String, color: String) extends StackRef

  class Parser extends RecipeParser with GenericConfigParser {
    def specMaterial = "M" ~> ":" ~> ident ^^ StackMaterial
    def specPart = "P" ~> ":" ~> ident ^^ StackPart
    def specPartColored = "C" ~> ":" ~> ident ~ "/" ~ ident ^^ { case name ~ sl ~ color => StackPartColored(name, color) }
    override def spec = specMaterial | specPart | specPartColored | super.spec
  }

  val loader = new RecipeLoader with GenericConfigLoader {
    val cfgStore = Tuning
    override def newParser() = new Parser

    val materials = AE2Defs.materials
    val parts = AE2Defs.parts

    def getOptionalStack(s: Optional[ItemStack], name: String): ItemStack = {
      if (s.isPresent)
        s.get()
      else
        error("Unable to resolve %s", name)
    }

    override def getConcreteStack(s: StackRef, cnt: Int): ItemStack = s match {
      case StackMaterial(name) =>
        getOptionalStack(AE2Defs.material(name).asInstanceOf[IItemDefinition].maybeStack(cnt), s.toString)
      case StackPart(name) =>
        getOptionalStack(AE2Defs.part(name).asInstanceOf[IItemDefinition].maybeStack(cnt), s.toString)
      case StackPartColored(name, colorName) =>
        val color = AEColor.valueOf(colorName)
        AE2Defs.part(name).asInstanceOf[AEColoredItemDefinition].stack(color, cnt)
      case _ => super.getConcreteStack(s, cnt)
    }
  }

  def loadDelayed(): Unit = loader.processRecipeStatements()

  def loadConfigFiles(): Unit = {
    if (!AE2Stuff.configDir.exists()) {
      AE2Stuff.configDir.mkdir()
      new File(AE2Stuff.configDir + "/overrides/").mkdir()
      exportConfigFile("recipes.cfg")
      exportConfigFile("tuning.cfg")
    }

    RecipesHelper.loadConfigs(
      modName = "AE2 Stuff",
      listResource = "/assets/ae2stuff/config/files.lst",
      configDir = AE2Stuff.configDir,
      resBaseName = "/assets/ae2stuff/config/",
      loader = loader)
  }

  private def exportConfigFile(fileName: String): Unit = {
    val is = getClass.getResourceAsStream("/assets/ae2stuff/config/" + fileName)
    Files.copy(is, Paths.get(AE2Stuff.configDir + "/overrides/" + fileName), StandardCopyOption.REPLACE_EXISTING)
  }
}


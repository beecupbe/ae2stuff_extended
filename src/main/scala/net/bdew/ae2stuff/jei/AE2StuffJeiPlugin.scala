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

package net.bdew.ae2stuff.jei

import mezz.jei.api.recipe.VanillaRecipeCategoryUid
import mezz.jei.api.{IModPlugin, IModRegistry, JEIPlugin}
import net.bdew.ae2stuff.machines.inscriber.BlockInscriber
import net.bdew.ae2stuff.machines.inscriber_adv.BlockInscriberAdv
import net.bdew.ae2stuff.machines.inscriber_imp.BlockInscriberImp
import net.bdew.ae2stuff.machines.inscriber_per.BlockInscriberPer
import net.minecraft.item.ItemStack

@JEIPlugin
class AE2StuffJeiPlugin extends IModPlugin {
  override def register(registry: IModRegistry): Unit = {
    registry.getRecipeTransferRegistry.addRecipeTransferHandler(EncoderTransferHandler, VanillaRecipeCategoryUid.CRAFTING)

    registry.addRecipeCatalyst(new ItemStack(BlockInscriber), "appliedenergistics2.inscriber")
    registry.addRecipeCatalyst(new ItemStack(BlockInscriberImp), "appliedenergistics2.inscriber")
    registry.addRecipeCatalyst(new ItemStack(BlockInscriberAdv), "appliedenergistics2.inscriber")
    registry.addRecipeCatalyst(new ItemStack(BlockInscriberPer), "appliedenergistics2.inscriber")
  }
}

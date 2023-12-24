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

package net.bdew.ae2stuff.machines.inscriber_per

import appeng.api.AEApi
import appeng.api.config.Upgrades
import appeng.api.features.{IInscriberRecipe, InscriberProcessType}
import appeng.api.networking.GridNotification
import appeng.util.Platform
import com.google.common.collect.Lists
import net.bdew.ae2stuff.grid.{GridTile, PoweredTile}
import net.bdew.ae2stuff.misc.UpgradeInventory
import net.bdew.lib.PimpVanilla._
import net.bdew.lib.block.TileKeepData
import net.bdew.lib.data.base.{TileDataSlots, UpdateKind}
import net.bdew.lib.data.{DataSlotBoolean, DataSlotFloat, DataSlotOption}
import net.bdew.lib.items.ItemUtils
import net.bdew.lib.tile.inventory.{PersistentInventoryTile, SidedInventory}
import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class TileInscriberPer extends TileDataSlots with GridTile with SidedInventory with PersistentInventoryTile with PoweredTile with TileKeepData {

  override def getSizeInventory: Int = 4
  override def getMachineRepresentation: ItemStack = new ItemStack(BlockInscriberPer)
  override def powerCapacity: Double = MachineInscriberPer.powerCapacity

  object slots {
    val top = 0
    val middle = 1
    val bottom = 2
    val output = 3
  }

  val upgrades = new UpgradeInventory("upgrades", this, 5, Set(Upgrades.SPEED))
  val progress = DataSlotFloat("progress", this).setUpdate(UpdateKind.SAVE, UpdateKind.GUI)
  val output = DataSlotOption[ItemStack]("output", this).setUpdate(UpdateKind.SAVE)

  val topLocked = DataSlotBoolean("topLocked", this, true).setUpdate(UpdateKind.SAVE, UpdateKind.GUI)
  val bottomLocked = DataSlotBoolean("bottomLocked", this, true).setUpdate(UpdateKind.SAVE, UpdateKind.GUI)

  persistLoad.listen(tag => {
    // This forces the values to be true when loading from older versions
    if (!tag.hasKey("topLocked")) topLocked := true
    if (!tag.hasKey("bottomLocked")) bottomLocked := true
  })

  private def isWorking = output.isDefined

  serverTick.listen(() => {
    if (isAwake) {
      if (!isWorking) {
        // No progress going - try starting
        findFinalRecipe foreach { recipe =>
          output.set(recipe.getOutput.copy())
          progress := 0
          decrStackSize(slots.middle, 1)
          if (recipe.getProcessType == InscriberProcessType.PRESS) {
            decrStackSize(slots.top, 1)
            decrStackSize(slots.bottom, 1)
          }
        }
        if (!isWorking) { // Failed - try name mold now
          val plateA = inv(slots.top)
          val plateB = inv(slots.bottom)
          val input = inv(slots.middle)

          if (!input.isEmpty) {
            val namePress = AEApi.instance().definitions().materials().namePress()
            val isNameA = namePress.isSameAs(plateA)
            val isNameB = namePress.isSameAs(plateB)
            var recipe: IInscriberRecipe = null
            if ((isNameA && isNameB) || isNameA && plateB.isEmpty) {
              recipe = makeNamePressRecipe(input, plateA, plateB)
            } else if (plateA.isEmpty && isNameB) {
              recipe = makeNamePressRecipe(input, plateB, plateA)
            }
            if (recipe != null) {
              output.set(recipe.getOutput.copy())
              progress := 0
              decrStackSize(slots.middle, 1)
            }
          }
        }

        if (!isWorking) // Failed - go sleep
          sleep()
      }
      if (isWorking) {
        // Have something to do
        if (progress < 1) {
          // Not finished - try progressing
          val progressPerTick = ((1F / MachineInscriberPer.cycleTicks) * (1 + upgrades.cards(Upgrades.SPEED))) * 30F
          val powerNeeded = (MachineInscriberPer.cyclePower * progressPerTick) / 4
          if (powerStored >= powerNeeded) {
            // Have enough power - consume it and add to progress
            progress += progressPerTick
            powerStored -= powerNeeded
          } else {
            // Not enough power - sleep
            sleep()
          }
        }
        if (progress >= 1) {
          // Finished - try to output
          output foreach { toOutput =>
            val oStack = getStackInSlot(slots.output)
            if (oStack.isEmpty || (ItemUtils.isSameItem(oStack, toOutput) && oStack.getCount + toOutput.getCount <= oStack.getMaxStackSize)) {
              // Can output - finish process
              if (oStack.isEmpty) {
                setInventorySlotContents(slots.output, toOutput)
              } else {
                oStack.grow(toOutput.getCount)
                markDirty()
              }
              output.unset()
              progress := 0
            } else {
              // Can't output - switch to sleep mode
              sleep()
            }
          }
        }
        requestPowerIfNeeded()
      }
    }
  })

  override def afterTileBreakSave(t: NBTTagCompound): NBTTagCompound = {
    t.removeTag("ae_node")
    t
  }

  override def onGridNotification(p1: GridNotification): Unit = {
    wakeup()
  }

  override def markDirty(): Unit = {
    wakeup()
    super.markDirty()
  }

  allowSided = true

  import scala.collection.JavaConversions._

  private def findFinalRecipe: Option[IInscriberRecipe] =
    AEApi.instance().registries().inscriber().getRecipes find isMatchingFullRecipe

  private def isMatchingFullRecipe(rec: IInscriberRecipe) =
    !getStackInSlot(slots.middle).isEmpty &&
      ItemUtils.isSameItem(rec.getTopOptional.orElse(ItemStack.EMPTY), getStackInSlot(slots.top)) &&
      ItemUtils.isSameItem(rec.getBottomOptional.orElse(ItemStack.EMPTY), getStackInSlot(slots.bottom)) &&
      rec.getInputs.exists(rs => ItemUtils.isSameItem(rs, getStackInSlot(slots.middle)))

  private def isMatchingPartialRecipe(rec: IInscriberRecipe, top: Option[ItemStack], middle: Option[ItemStack], bottom: Option[ItemStack]): Boolean = {
    if (top.isDefined) {
      if (!rec.getTopOptional.isPresent) return false
      if (!ItemUtils.isSameItem(rec.getTopOptional.get(), top.get)) return false
    }
    if (middle.isDefined) {
      if (!rec.getInputs.exists(rs => ItemUtils.isSameItem(rs, middle.get))) return false
    }
    if (bottom.isDefined) {
      if (!rec.getBottomOptional.isPresent) return false
      if (!ItemUtils.isSameItem(rec.getBottomOptional.get(), bottom.get)) return false
    }
    true
  }

  private def isValidPartialRecipe(top: Option[ItemStack], middle: Option[ItemStack], bottom: Option[ItemStack]): Boolean = {
    AEApi.instance().registries().inscriber().getRecipes.exists(rec => isMatchingPartialRecipe(rec, top, middle, bottom))
  }

  private def stackOption(s: ItemStack): Option[ItemStack] = if (s.isEmpty) None else Some(s)

  override def isItemValidForSlot(slot: Int, stack: ItemStack): Boolean = slot match {
    case slots.top =>
      if (isNamePress(stack)) {
        return true
      }
      isValidPartialRecipe(Some(stack), stackOption(inv(slots.middle)), stackOption(inv(slots.bottom)))

    case slots.bottom =>
      if (isNamePress(stack)) {
        return true
      }
      isValidPartialRecipe(stackOption(inv(slots.top)), stackOption(inv(slots.middle)), Some(stack))

    case slots.middle =>
      if (isNamePress(inv(slots.top)) || isNamePress(inv(slots.bottom))) {
        return true
      }
      isValidPartialRecipe(stackOption(inv(slots.top)), Some(stack), stackOption(inv(slots.bottom)))

    case _ => false
  }

  private def isNamePress(stack: ItemStack): Boolean = AEApi.instance().definitions().materials().namePress().isSameAs(stack)

  override def canExtractItem(slot: Int, stack: ItemStack, side: EnumFacing): Boolean = slot match {
    case slots.output => true
    case slots.top => (!topLocked) && (!output.isDefined) && inv(slots.middle).isEmpty
    case slots.bottom => (!bottomLocked) && (!output.isDefined) && inv(slots.middle).isEmpty
    case _ => false
  }

  override def shouldRefresh(world: World, pos: BlockPos, oldState: IBlockState, newSate: IBlockState): Boolean = newSate.getBlock != BlockInscriberPer

  onWake.listen(() => {
    if (world.isBlockLoaded(pos)) {
      BlockInscriberPer.setActive(world, pos, true)
    }
  })
  onSleep.listen(() => {
    if (world.isBlockLoaded(pos)) {
      BlockInscriberPer.setActive(world, pos, false)
    }
  })

  private def makeNamePressRecipe(input: ItemStack, plateA: ItemStack, plateB: ItemStack): IInscriberRecipe = {
    var name = ""

    if (!plateA.isEmpty) {
      val tag = Platform.openNbtData(plateA)
      name += tag.getString("InscribeName")
    }

    if (!plateB.isEmpty) {
      val tag = Platform.openNbtData(plateB)
      name += " " + tag.getString("InscribeName")
    }

    val startingItem = input.copy
    val renamedItem = input.copy
    val tag = Platform.openNbtData(renamedItem)

    val display = tag.getCompoundTag("display")
    tag.setTag("display", display)

    if (name.nonEmpty) {
      display.setString("Name", name)
    } else {
      display.removeTag("Name")
    }

    val inputs = Lists.newArrayList(startingItem)
    val processType = InscriberProcessType.INSCRIBE

    val builder = AEApi.instance().registries().inscriber().builder()
    builder.withInputs(inputs).withOutput(renamedItem).withProcessType(processType)

    if (!plateA.isEmpty) {
      builder.withTopOptional(plateA)
    }

    if (!plateB.isEmpty) {
      builder.withBottomOptional(plateB)
    }

    builder.build()
  }
}

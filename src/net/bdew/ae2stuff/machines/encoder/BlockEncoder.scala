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

package net.bdew.ae2stuff.machines.encoder

import net.bdew.ae2stuff.AE2Stuff
import net.bdew.ae2stuff.misc.{BlockActiveTexture, BlockWrenchable, MachineMaterial}
import net.bdew.lib.block.{BaseBlock, BlockKeepData, HasTE}
import net.bdew.lib.rotate.{BlockFacingMeta, Properties}
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util._
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object BlockEncoder extends BaseBlock("encoder", MachineMaterial) with HasTE[TileEncoder] with BlockWrenchable with BlockFacingMeta with BlockKeepData with BlockActiveTexture {
  override val TEClass = classOf[TileEncoder]

  setHardness(2)

  override def getStateFromMeta(meta: Int) =
    getDefaultState
      .withProperty(Properties.FACING, EnumFacing.getFront(meta & 7))
      .withProperty(BlockActiveTexture.Active, Boolean.box((meta & 8) > 0))

  override def getMetaFromState(state: IBlockState) = {
    state.getValue(Properties.FACING).ordinal() | (if (state.getValue(BlockActiveTexture.Active)) 8 else 0)
  }

  override def onBlockActivatedReal(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, hand: EnumHand, heldItem: ItemStack, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (getTE(world, pos).getNode.isActive) {
      player.openGui(AE2Stuff, MachineEncoder.guiId, world, pos.getX, pos.getY, pos.getZ)
    } else {
      import net.bdew.lib.helpers.ChatHelper._
      player.sendStatusMessage(L("ae2stuff.error.not_connected").setColor(Color.RED), true)
    }
    true
  }

  override def onBlockPlacedBy(world: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: ItemStack): Unit = {
    super.onBlockPlacedBy(world, pos, state, placer, stack)
    if (placer.isInstanceOf[EntityPlayer])
      getTE(world, pos).placingPlayer = placer.asInstanceOf[EntityPlayer]
  }
}

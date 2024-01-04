package net.bdew.ae2stuff.machines.wireless

import net.minecraftforge.client.model.ModelLoaderRegistry
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

@SideOnly(Side.CLIENT)
object WirelessClientModelLoader {
  def init() {
    ModelLoaderRegistry.registerLoader(new WirelessModelLoader(Map("models/block/builtin/wireless" -> new WirelessModelFactory())))
  }
}

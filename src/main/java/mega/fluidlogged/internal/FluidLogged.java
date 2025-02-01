/*
 * This file is part of FluidLogged.
 *
 * Copyright (C) 2025 The MEGA Team, FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice, this permission notice and the word "MEGA"
 * shall be included in all copies or substantial portions of the Software.
 *
 * FluidLogged is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * FluidLogged is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FluidLogged.  If not, see <https://www.gnu.org/licenses/>.
 */

package mega.fluidlogged.internal;

import com.falsepattern.chunk.api.DataRegistry;
import lombok.val;
import mega.fluidlogged.Tags;

import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = Tags.MOD_ID,
     version = Tags.MOD_VERSION,
     name = Tags.MOD_NAME,
     acceptedMinecraftVersions = "[1.7.10]",
     dependencies = "required-after:chunkapi@[0.6.0,);")
public class FluidLogged {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        DataRegistry.registerDataManager(new FLManager());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onBucketEvent(FillBucketEvent event) {
        val hit = event.target;
        if (hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        val newBucket = FLUtil.doFluidLog(event.current, event.world, hit.blockX, hit.blockY, hit.blockZ);
        if (newBucket != null) {
            event.result = newBucket;
            event.setResult(Event.Result.ALLOW);
        }
    }
}

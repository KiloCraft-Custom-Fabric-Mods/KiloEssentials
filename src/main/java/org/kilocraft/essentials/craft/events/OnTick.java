package org.kilocraft.essentials.craft.events;

import java.util.List;

import org.kilocraft.essentials.api.KiloServer;
import org.kilocraft.essentials.api.event.EventHandler;
import org.kilocraft.essentials.api.event.serverEvents.ServerEvent$OnTick;
import net.minecraft.block.Blocks;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;

public class OnTick implements EventHandler<ServerEvent$OnTick> {

	@Override
	public void handle(ServerEvent$OnTick event) {
		processParticles(event);
	}

	private void processParticles(ServerEvent$OnTick event) {
		ServerWorld world = event.getServer().getWorld(DimensionType.OVERWORLD);
		List<ServerPlayerEntity> players = world.getPlayers();
		// TODO in future could we organize this.
		for (int i = 0; i < players.size(); i++) { // Couldn't we just iterate through all ServerPlayerEntity(s) online.
			int particle = KiloServer.getServer().getUserManager().getUser(players.get(i).getUuid()).getDisplayParticleId();
			if (particle != 0) {
				for (int j = 0; j < players.size(); j++) {
					if (particle == 1) {
						 //Player, visable to only you, position (X, Y, Z), speed, delta (X, Y, Z), count
						world.spawnParticles(players.get(j), ParticleTypes.LAVA, true, players.get(i).getX(),
								players.get(i).getY(), players.get(i).getZ(), 0, 0.25f, 0, 0.25f, 1);
					} else if (particle == 2) {
						world.spawnParticles(players.get(j),
								new BlockStateParticleEffect(ParticleTypes.BLOCK,
										Blocks.WHITE_STAINED_GLASS.getDefaultState()),
								false, players.get(i).getX(), players.get(i).getY(), players.get(i).getZ(), 2, 0.25f, 0,
								0.25f, 2);
						world.spawnParticles(players.get(j),
								new BlockStateParticleEffect(ParticleTypes.BLOCK,
										Blocks.YELLOW_STAINED_GLASS.getDefaultState()),
								false, players.get(i).getX(), players.get(i).getY(), players.get(i).getZ(), 2, 0.25f, 0,
								0.25f, 2);
						world.spawnParticles(players.get(j),
								new BlockStateParticleEffect(ParticleTypes.BLOCK,
										Blocks.ORANGE_STAINED_GLASS.getDefaultState()),
								false, players.get(i).getX(), players.get(i).getY(), players.get(i).getZ(), 2, 0.25f, 0,
								0.25f, 2);
						world.spawnParticles(players.get(j),
								new BlockStateParticleEffect(ParticleTypes.BLOCK,
										Blocks.RED_STAINED_GLASS.getDefaultState()),
								false, players.get(i).getX(), players.get(i).getY(), players.get(i).getZ(), 2, 0.25f, 0,
								0.25f, 2);
					} else if (particle == 3) {
						world.spawnParticles(players.get(j), ParticleTypes.CLOUD, true, players.get(i).getX(),
								players.get(i).getY() + 3.5f, players.get(i).getZ(), 10, 0.6f, 0.15f, 0.6f, 0);
						world.spawnParticles(players.get(j), ParticleTypes.RAIN, true, players.get(i).getX(),
								players.get(i).getY() + 3.55f, players.get(i).getZ(), 3, 0.4f, 0f, 0.4f, 2);
					}
				}
			}
		}
	}

}

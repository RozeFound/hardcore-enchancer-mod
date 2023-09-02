package com.github.RozeFound.hardcore;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import static net.minecraft.server.command.CommandManager.*;

import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;


public class Main implements ModInitializer {

	public int lifesGetHandler(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) throws CommandSyntaxException {

		int deaths = getDeaths(player);
		int hearts = (int)player.getMaxHealth() / 2;
		
		var string = String.format("%s died %d times and have %d lifes", player.getEntityName(), deaths, hearts);
		
		context.getSource().sendMessage(Text.literal(string));

		return 1;

	}

	public int lifesSetHandler(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {

		var player = EntityArgumentType.getPlayer(context, "player");
		var lifes = context.getArgument("lifes", Integer.class);

		setDeaths(player, 10 - lifes);
		setMaxHealth(player, lifes * 2);

		return 1;

	}

	public int lifesGiveHandler(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {

		var gifter = context.getSource().getPlayer();
		var player = EntityArgumentType.getPlayer(context, "player");

		if (gifter == player) {
			context.getSource().sendError(Text.literal("You can't gift yourself lifes"));
			return -1;
		}

		var lifes = context.getArgument("lifes", Integer.class);

		if (lifes > 10 - getDeaths(gifter)) {
			context.getSource().sendError(Text.literal("You don't have enough lifes"));
			return -1;
		} else if (lifes > getDeaths(player)) lifes = getDeaths(player);

		if (lifes == 0) { 
			context.getSource().sendError(Text.literal("You can't give 0 lifes"));
			return -1;
		}

		gifter.increaseStat(Stats.DEATHS, lifes);
		player.increaseStat(Stats.DEATHS, -lifes);

		if (getDeaths(player) == 9) {
			var spawn_coords = player.getServer().getWorld(World.OVERWORLD).getSpawnPos();
			player.teleport(spawn_coords.getX(), spawn_coords.getY(), spawn_coords.getZ());
			player.changeGameMode(GameMode.SURVIVAL);
		}
		
		setMaxHealth(gifter, 20 - (getDeaths(gifter) * 2));
		setMaxHealth(player, 20 - (getDeaths(player) * 2));

		var string = String.format("Given %d lifes to %s", lifes, player.getEntityName());
		context.getSource().sendMessage(Text.literal(string));

		return 1;

	}

	public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) { 

		var lifes_builder = literal("lifes");

		lifes_builder.then(literal("get")
			.then(argument("player", EntityArgumentType.player())
				.executes(ctx -> lifesGetHandler(ctx, EntityArgumentType.getPlayer(ctx, "player"))))
			.executes(ctx -> lifesGetHandler(ctx, ctx.getSource().getPlayer()))
		);

		lifes_builder.then(literal("set")
			.requires(source -> source.hasPermissionLevel(4))
			.then(argument("player", EntityArgumentType.player())
			.then(argument("lifes", IntegerArgumentType.integer())
			.executes(ctx -> lifesSetHandler(ctx))))
		);

		lifes_builder.then(literal("give")
			.then(argument("player", EntityArgumentType.player())
			.then(argument("lifes", IntegerArgumentType.integer())
			.executes(ctx -> lifesGiveHandler(ctx))))
		);

		dispatcher.register(literal("hardcore").then(lifes_builder));

	}

	public int getDeaths(ServerPlayerEntity player) {
		return player.getStatHandler().getStat(Stats.CUSTOM, Stats.DEATHS);
	}

	public void setDeaths(ServerPlayerEntity player, int deaths) {
		player.getStatHandler().setStat(player, Stats.CUSTOM.getOrCreateStat(Stats.DEATHS), deaths);
	}

	public void setMaxHealth(ServerPlayerEntity player, int maxHealth) {
		var attribute = player.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		attribute.setBaseValue(maxHealth);
		player.setHealth(Math.min(maxHealth, player.getHealth()));
	}

	public void onPlayerDeath(ServerPlayerEntity player, DamageSource source) {

		if (player.getPrimeAdversary() instanceof ServerPlayerEntity attacker) {

			if (attacker == player) return;

			attacker.increaseStat(Stats.DEATHS, 1);
			setMaxHealth(attacker, 20 - (getDeaths(attacker) * 2));

			if (getDeaths(attacker) == 10)
				attacker.kill();

			player.increaseStat(Stats.DEATHS, -2);

		}

	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
		PlayerDeathCallback.EVENT.register((player, source) -> onPlayerDeath(player, source));

	}
}
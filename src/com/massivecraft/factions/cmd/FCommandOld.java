package com.massivecraft.factions.cmd;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.massivecraft.factions.ConfServer;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.FFlag;
import com.massivecraft.factions.FPerm;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayerColl;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionColl;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.zcore.MCommand;
import com.massivecraft.mcore.util.Txt;


public abstract class FCommandOld extends MCommand<Factions>
{
	public FPlayer fme;
	public Faction myFaction;
	
	// TODO: All these are "command reqs"
	public boolean senderMustBeMember;
	public boolean senderMustBeOfficer;
	public boolean senderMustBeLeader;
	
	public boolean isMoneyCommand;
	
	public FCommandOld()
	{
		super(Factions.get());
		
		// The money commands must be disabled if money should not be used.
		isMoneyCommand = false;
		
		senderMustBeMember = false;
		senderMustBeOfficer = false;
		senderMustBeLeader = false;
	}
	
	@Override
	public void execute(CommandSender sender, List<String> args, List<MCommand<?>> commandChain)
	{
		if (sender instanceof Player)
		{
			this.fme = FPlayerColl.get().get(sender);
			this.myFaction = this.fme.getFaction();
		}
		else
		{
			this.fme = null;
			this.myFaction = null;
		}
		super.execute(sender, args, commandChain);
	}
	
	@Override
	public boolean isEnabled()
	{	
		if (this.isMoneyCommand && ! ConfServer.econEnabled)
		{
			msg("<b>Faction economy features are disabled on this server.");
			return false;
		}
		
		if (this.isMoneyCommand && ! ConfServer.bankEnabled)
		{
			msg("<b>The faction bank system is disabled on this server.");
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean validSenderType(CommandSender sender, boolean informSenderIfNot)
	{
		boolean superValid = super.validSenderType(sender, informSenderIfNot);
		if ( ! superValid) return false;
		
		if ( ! (this.senderMustBeMember || this.senderMustBeOfficer || this.senderMustBeLeader)) return true;
		
		if ( ! (sender instanceof Player)) return false;
		
		FPlayer fplayer = FPlayerColl.get().get((Player)sender);
		
		if ( ! fplayer.hasFaction())
		{
			sender.sendMessage(Txt.parse("<b>You are not member of any faction."));
			return false;
		}
		
		if (this.senderMustBeOfficer && ! fplayer.getRole().isAtLeast(Rel.OFFICER))
		{
			sender.sendMessage(Txt.parse("<b>Only faction moderators can %s.", this.getHelpShort()));
			return false;
		}
		
		if (this.senderMustBeLeader && ! fplayer.getRole().isAtLeast(Rel.LEADER))
		{
			sender.sendMessage(Txt.parse("<b>Only faction admins can %s.", this.getHelpShort()));
			return false;
		}
			
		return true;
	}
	
	// -------------------------------------------- //
	// Assertions
	// -------------------------------------------- //

	// These are not used. Remove in the future if no need for them arises.
	
	/*
	public boolean assertHasFaction()
	{
		if (me == null) return true;
		
		if ( ! fme.hasFaction())
		{
			sendMessage("You are not member of any faction.");
			return false;
		}
		return true;
	}

	public boolean assertMinRole(Rel role)
	{
		if (me == null) return true;
		
		if (fme.getRole().isLessThan(role))
		{
			msg("<b>You <h>must be "+role+"<b> to "+this.getHelpShort()+".");
			return false;
		}
		return true;
	}*/
	
	// -------------------------------------------- //
	// Argument Readers
	// -------------------------------------------- //
	
	// TODO: Convert these arg-readers to MCore ArgReaders.
	
	// FPLAYER ======================
	public FPlayer strAsFPlayer(String name, FPlayer def, boolean msg)
	{
		FPlayer ret = def;
		
		if (name != null)
		{
			FPlayer fplayer = FPlayerColl.get().get(name);
			if (fplayer != null)
			{
				ret = fplayer;
			}
		}
		
		if (msg && ret == null)
		{
			this.msg("<b>No player \"<p>%s<b>\" could be found.", name);			
		}
		
		return ret;
	}
	public FPlayer argAsFPlayer(int idx, FPlayer def, boolean msg)
	{
		return this.strAsFPlayer(this.argAsString(idx), def, msg);
	}
	public FPlayer argAsFPlayer(int idx, FPlayer def)
	{
		return this.argAsFPlayer(idx, def, true);
	}
	public FPlayer argAsFPlayer(int idx)
	{
		return this.argAsFPlayer(idx, null);
	}
	
	// BEST FPLAYER MATCH ======================
	public FPlayer strAsBestFPlayerMatch(String name, FPlayer def, boolean msg)
	{
		FPlayer ret = def;
		
		if (name != null)
		{
			// TODO: Easy fix for now
			//FPlayer fplayer = FPlayerColl.get().getBestIdMatch(name);
			FPlayer fplayer = FPlayerColl.get().getId2entity().get(name);
			if (fplayer != null)
			{
				ret = fplayer;
			}
		}
		
		if (msg && ret == null)
		{
			this.msg("<b>No player match found for \"<p>%s<b>\".", name);
		}
		
		return ret;
	}
	public FPlayer argAsBestFPlayerMatch(int idx, FPlayer def, boolean msg)
	{
		return this.strAsBestFPlayerMatch(this.argAsString(idx), def, msg);
	}
	public FPlayer argAsBestFPlayerMatch(int idx, FPlayer def)
	{
		return this.argAsBestFPlayerMatch(idx, def, true);
	}
	public FPlayer argAsBestFPlayerMatch(int idx)
	{
		return this.argAsBestFPlayerMatch(idx, null);
	}
	
	// FACTION ======================
	public Faction strAsFaction(String name, Faction def, boolean msg)
	{
		Faction ret = def;
		
		if (name != null)
		{
			Faction faction = null;
			
			// First we try an exact match
			if (faction == null)
			{
				faction = FactionColl.get().getByTag(name);
			}
			
			// Next we match faction tags
			if (faction == null)
			{
				faction = FactionColl.get().getBestTagMatch(name);
			}
				
			// Next we match player names
			if (faction == null)
			{
				// TODO: Easy fix for now
				//FPlayer fplayer = FPlayerColl.get().getBestIdMatch(name);
				FPlayer fplayer = FPlayerColl.get().getId2entity().get(name);
				
				if (fplayer != null)
				{
					faction = fplayer.getFaction();
				}
			}
			
			if (faction != null)
			{
				ret = faction;
			}
		}
		
		if (msg && ret == null)
		{
			this.msg("<b>The faction or player \"<p>%s<b>\" could not be found.", name);
		}
		
		return ret;
	}
	public Faction argAsFaction(int idx, Faction def, boolean msg)
	{
		return this.strAsFaction(this.argAsString(idx), def, msg);
	}
	public Faction argAsFaction(int idx, Faction def)
	{
		return this.argAsFaction(idx, def, true);
	}
	public Faction argAsFaction(int idx)
	{
		return this.argAsFaction(idx, null);
	}
	
	// FACTION FLAG ======================
	public FFlag strAsFactionFlag(String name, FFlag def, boolean msg)
	{
		FFlag ret = def;
		
		if (name != null)
		{
			FFlag flag = FFlag.parse(name);
			if (flag != null)
			{
				ret = flag;
			}
		}
		
		if (msg && ret == null)
		{
			this.msg("<b>The faction-flag \"<p>%s<b>\" could not be found.", name);
		}
		
		return ret;
	}
	public FFlag argAsFactionFlag(int idx, FFlag def, boolean msg)
	{
		return this.strAsFactionFlag(this.argAsString(idx), def, msg);
	}
	public FFlag argAsFactionFlag(int idx, FFlag def)
	{
		return this.argAsFactionFlag(idx, def, true);
	}
	public FFlag argAsFactionFlag(int idx)
	{
		return this.argAsFactionFlag(idx, null);
	}
	
	// FACTION PERM ======================
	public FPerm strAsFactionPerm(String name, FPerm def, boolean msg)
	{
		FPerm ret = def;
		
		if (name != null)
		{
			FPerm perm = FPerm.parse(name);
			if (perm != null)
			{
				ret = perm;
			}
		}
		
		if (msg && ret == null)
		{
			this.msg("<b>The faction-perm \"<p>%s<b>\" could not be found.", name);
		}
		
		return ret;
	}
	public FPerm argAsFactionPerm(int idx, FPerm def, boolean msg)
	{
		return this.strAsFactionPerm(this.argAsString(idx), def, msg);
	}
	public FPerm argAsFactionPerm(int idx, FPerm def)
	{
		return this.argAsFactionPerm(idx, def, true);
	}
	public FPerm argAsFactionPerm(int idx)
	{
		return this.argAsFactionPerm(idx, null);
	}
	
	// FACTION REL ======================
	public Rel strAsRel(String name, Rel def, boolean msg)
	{
		Rel ret = def;
		
		if (name != null)
		{
			Rel perm = Rel.parse(name);
			if (perm != null)
			{
				ret = perm;
			}
		}
		
		if (msg && ret == null)
		{
			this.msg("<b>The role \"<p>%s<b>\" could not be found.", name);
		}
		
		return ret;
	}
	public Rel argAsRel(int idx, Rel def, boolean msg)
	{
		return this.strAsRel(this.argAsString(idx), def, msg);
	}
	public Rel argAsRel(int idx, Rel def)
	{
		return this.argAsRel(idx, def, true);
	}
	public Rel argAsRel(int idx)
	{
		return this.argAsRel(idx, null);
	}
	
	
}
package client.command.external;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import constants.ParanoiaConstants;
import constants.ServerConstants;
import server.MapleInventoryManipulator;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.MapleLogger;
import tools.MaplePacketCreator;
import net.server.Channel;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;

public class DonorCommands extends EnumeratedCommands {
	private static final int gmLevel = 1;
	private static final char heading = '@';
	
	@SuppressWarnings("unused")
	public static boolean execute(MapleClient c, String[] sub, char heading) {
		MapleCharacter chr = c.getPlayer();
		Channel cserv = c.getChannelServer();
		MapleCharacter victim; // For commands with targets.
		ResultSet rs; // For commands with MySQL results.

		try {
			Command command = Command.valueOf(sub[0]);
			switch (command) {
				default:
					// chr.yellowMessage("Command: " + heading + sub[0] + ": does not exist.");
					return false;
				case donor:
					chr.setHp(30000);
					chr.setMaxHp(30000);
					chr.setMp(30000);
					chr.setMaxMp(30000);
					chr.updateSingleStat(MapleStat.HP, 30000);
					chr.updateSingleStat(MapleStat.MAXHP, 30000);
					chr.updateSingleStat(MapleStat.MP, 30000);
					chr.updateSingleStat(MapleStat.MAXMP, 30000);
					chr.message("Who's awesome? You're awesome!");
					break;
				case heal:
					chr.setHp(chr.getMaxHp());
					chr.setMp(chr.getMaxMp());
					chr.updateSingleStat(MapleStat.HP, chr.getMaxHp());
					chr.updateSingleStat(MapleStat.MP, chr.getMaxMp());
					chr.message("Healed for free. Thanks for your donation!");
					break;
				case help:
					if (sub.length > 1) {
						if (sub[1].equalsIgnoreCase("donor")) {
							if (sub.length > 2 && ServerConstants.PAGINATE_HELP) {
								getHelp(Integer.parseInt(sub[2]), chr);
							} else {
								getHelp(chr);
							}
							break;
						} else {
							return false;
						}
					} else {
						return false;
					}
				case itemvac:
					List<MapleMapObject> items = chr.getMap().getMapObjectsInRange(chr.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM));
					for (MapleMapObject item : items) {
						MapleMapItem mapitem = (MapleMapItem) item;
						if (!MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
							continue;
						}
						mapitem.setPickedUp(true);
						chr.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()), mapitem.getPosition());
						chr.getMap().removeMapObject(item);
						chr.getMap().nullifyObject(item);
					}
					break;
			}
			if (ServerConstants.USE_PARANOIA && ParanoiaConstants.PARANOIA_COMMAND_LOGGER && ParanoiaConstants.LOG_DONOR_COMMANDS) {
				MapleLogger.printFormatted(MapleLogger.PARANOIA_COMMAND, "[" + c.getPlayer().getName() + "] Used " + heading + sub[0] + ((sub.length > 1) ? " with parameters: " + joinStringFrom(sub, 1) : "."));
			}
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	protected static void getHelp(MapleCharacter chr) {
		DonorCommands.getHelp(-1, chr);
	}

	protected static void getHelp(int page, MapleCharacter chr) {
        int pageNumber = (int) (Command.values().length / ServerConstants.ENTRIES_PER_PAGE);
        if (Command.values().length % ServerConstants.ENTRIES_PER_PAGE > 0) {
        	pageNumber++;
        }
		if (page <= 0 || pageNumber == 1) {
			chr.dropMessage(ServerConstants.SERVER_NAME + "'s DonorCommands Help");
			for (Command cmd : Command.values()) {
				chr.dropMessage(heading + cmd.name() + " - " + cmd.getDescription());
			}
		} else {
			if (page > pageNumber) {
				page = pageNumber;
			}
			int lastPageEntry = Math.min(0, (page - 1) * ServerConstants.ENTRIES_PER_PAGE);
			chr.dropMessage(ServerConstants.SERVER_NAME + "'s DonorCommands Help (Page " + page + " / " + pageNumber + ")");
			for (int i = lastPageEntry; i < lastPageEntry + ServerConstants.ENTRIES_PER_PAGE; i++) {
				chr.dropMessage(heading + Command.values()[i].name() + " - " + Command.values()[i].getDescription());
			}
		}
	}
	
	public static int getRequiredStaffRank() {
		return gmLevel;
	}
	
	public static char getHeading() {
		return heading;
	}

	private static enum Command {
		donor("Rewards you for donating!"), 
		heal("Heals you, for free!"),
		help("Displays this help message."),
		itemvac("Vacuums up all the items on the map.");

	    private final String description;
	    
	    private Command(String description){
	        this.description = description;
	    }
	    
	    public String getDescription() {
	    	return this.description;
	    }
	}
}
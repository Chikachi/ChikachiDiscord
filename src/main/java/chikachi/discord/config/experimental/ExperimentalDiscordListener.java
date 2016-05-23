package chikachi.discord.config.experimental;

import chikachi.discord.DiscordClient;
import chikachi.discord.config.Configuration;
import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.user.UserOnlineStatusUpdateEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.FakePlayer;

import java.util.HashMap;
import java.util.List;

public class ExperimentalDiscordListener extends ListenerAdapter {
    private final MinecraftServer minecraftServer;
    private HashMap<String, FakePlayer> fakePlayers = new HashMap<>();

    public ExperimentalDiscordListener(MinecraftServer minecraftServer) {
        this.minecraftServer = minecraftServer;
    }

    private void userOnline(User user) {
        if (user == null) return;
        if (user.getOnlineStatus() == OnlineStatus.OFFLINE) return;
        if (!Configuration.isExperimentalFakePlayersEnabled()) return;

        DiscordFakePlayer discordFakePlayer = new DiscordFakePlayer(this.minecraftServer, user);

        this.minecraftServer.getPlayerList().playerLoggedIn(discordFakePlayer);

        fakePlayers.put(user.getUsername(), discordFakePlayer);
    }

    private void userOffline(User user) {
        if (user == null) return;
        if (user.getOnlineStatus() != OnlineStatus.OFFLINE) return;
        if (!Configuration.isExperimentalFakePlayersEnabled()) return;

        if (fakePlayers.containsKey(user.getUsername())) {
            FakePlayer fakePlayer = fakePlayers.get(user.getUsername());
            if (fakePlayer != null) {
                this.minecraftServer.getPlayerList().playerLoggedOut(fakePlayer);
                fakePlayers.remove(user.getUsername());
            }
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        DiscordClient client = DiscordClient.getInstance();

        TextChannel channel = client.getChannel();
        if (channel == null) {
            return;
        }

        if (Configuration.isExperimentalFakePlayersEnabled()) {
            List<User> users = channel.getUsers();
            users.forEach(this::userOnline);
        }
    }

    @Override
    public void onUserOnlineStatusUpdate(UserOnlineStatusUpdateEvent event) {
        if (!Configuration.isExperimentalFakePlayersEnabled()) return;

        User user = event.getUser();
        OnlineStatus before = event.getPreviousOnlineStatus();
        OnlineStatus now = user.getOnlineStatus();

        if (before == OnlineStatus.OFFLINE && now != OnlineStatus.OFFLINE) {
            userOnline(user);
        } else if (before != OnlineStatus.OFFLINE && now == OnlineStatus.OFFLINE) {
            userOffline(user);
        }
    }
}
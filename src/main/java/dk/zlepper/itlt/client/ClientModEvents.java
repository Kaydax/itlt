package dk.zlepper.itlt.client;

import com.google.gson.Gson;
import dk.zlepper.itlt.client.launchers.LauncherUtils;
import dk.zlepper.itlt.client.launchers.DetectedLauncher;
import dk.zlepper.itlt.itlt;
import dk.zlepper.itlt.client.helpers.ClientUtils;
import dk.zlepper.itlt.client.helpers.Message;

import net.minecraft.client.Minecraft;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;

import static dk.zlepper.itlt.client.ClientConfig.makeItltFolderIfNeeded;

@Mod.EventBusSubscriber(modid=itlt.MOD_ID, value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEvents {

    public static float currentMem = getCurrentMem();
    public static File itltDir = null;
    public static DetectedLauncher detectedLauncher = LauncherUtils.getDetectedLauncher();

    // get the maximum amount of RAM currently available for allocation to the JVM, including Permgen/Metaspace,
    // rounded to the nearest tenth (e.g. 1.0, 1.1, 1.2...)
    private static float getCurrentMem() {
        final long currentMem = Runtime.getRuntime().maxMemory() + ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getMax();
        return Float.parseFloat(String.format("%.1f", currentMem / 1073741824F));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) // run this as soon as possible to avoid wasting time if a requirement isn't met
    public static void commonInit(final FMLCommonSetupEvent event) {
        itltDir = makeItltFolderIfNeeded();

        final int javaVerInt = ClientUtils.getJavaVersion();
        itlt.LOGGER.debug("javaVerInt: " + javaVerInt);

        itlt.LOGGER.info("detectedLauncher: " + detectedLauncher.getName());

        // Minimum Java version requirement and warning
        itlt.LOGGER.debug("requiredMinJavaVerion: " + ClientConfig.requiredMinJavaVersion.get());
        itlt.LOGGER.debug("warnMinJavaVersion: " + ClientConfig.warnMinJavaVersion.get());
        if (ClientConfig.enableMinJavaVerRequirement.get() && javaVerInt < ClientConfig.requiredMinJavaVersion.get()) {
            ClientUtils.startUIProcess(Message.Content.NeedsNewerJava);
        } else if (ClientConfig.enableMinJavaVerWarning.get() && javaVerInt < ClientConfig.warnMinJavaVersion.get()) {
            if (ClientConfig.selectivelyIgnoreMinJavaVerWarning.get()) {
                if (!detectedLauncher.supportsChangingJavaVersion()) {
                    itlt.LOGGER.info("Skipping minJavaVerWarning as you appear to be using the " + detectedLauncher.getName()
                            + " launcher which currently does not allow changing Java version beyond Java 8. :(");
                    itlt.LOGGER.info("If you are seeing this and your launcher does allow it, update itlt.");
                    itlt.LOGGER.info("If already up-to-date, let us know by filing an issue on itlt's Github issues");
                }
                else ClientUtils.startUIProcess(Message.Content.WantsNewerJava);
            } else {
                ClientUtils.startUIProcess(Message.Content.WantsNewerJava);
            }
        }

        // Max Java version requirement and warning
        itlt.LOGGER.debug("requiredMaxJavaVerion: " + ClientConfig.requiredMaxJavaVersion.get());
        itlt.LOGGER.debug("warnMaxJavaVersion: " + ClientConfig.warnMaxJavaVersion.get());
        if (ClientConfig.enableMaxJavaVerRequirement.get() && javaVerInt > ClientConfig.requiredMaxJavaVersion.get()) {
            ClientUtils.startUIProcess(Message.Content.NeedsOlderJava);
        } else if (ClientConfig.enableMaxJavaVerWarning.get() && javaVerInt > ClientConfig.warnMaxJavaVersion.get()) {
            if (ClientConfig.selectivelyIgnoreMaxJavaVerWarning.get()) {
                if (!detectedLauncher.supportsChangingJavaVersion()) {
                    itlt.LOGGER.info("Skipping maxJavaVerWarning as you appear to be using the " + detectedLauncher.getName()
                            + " launcher which currently does not allow changing Java version beyond Java 8. :(");
                    itlt.LOGGER.info("If you are seeing this and your launcher does allow it, update itlt.");
                    itlt.LOGGER.info("If already up-to-date, let us know by filing an issue on itlt's Github issues");
                }
                else ClientUtils.startUIProcess(Message.Content.WantsOlderJava);
            } else {
                ClientUtils.startUIProcess(Message.Content.WantsOlderJava);
            }
        }

        // Memory-related requirements and warnings
        itlt.LOGGER.debug("currentMem: " + currentMem);
        itlt.LOGGER.debug("reqMinMemoryAmountInGB: " + ClientConfig.reqMinMemoryAmountInGB.get());
        itlt.LOGGER.debug("warnMinMemoryAmountInGB: " + ClientConfig.warnMinMemoryAmountInGB.get());
        itlt.LOGGER.debug("reqMaxMemoryAmountInGB: " + ClientConfig.reqMaxMemoryAmountInGB.get());
        itlt.LOGGER.debug("warnMaxMemoryAmountInGB: " + ClientConfig.warnMaxMemoryAmountInGB.get());

        if (ClientConfig.enableMinMemoryRequirement.get() && currentMem < ClientConfig.reqMinMemoryAmountInGB.get().floatValue())
            ClientUtils.startUIProcess(Message.Content.NeedsMoreMemory);
        else if (ClientConfig.enableMinMemoryWarning.get() && currentMem < ClientConfig.warnMinMemoryAmountInGB.get().floatValue())
            ClientUtils.startUIProcess(Message.Content.WantsMoreMemory);

        if (ClientConfig.enableMaxMemoryRequirement.get() && currentMem > ClientConfig.reqMaxMemoryAmountInGB.get().floatValue())
            ClientUtils.startUIProcess(Message.Content.NeedsLessMemory);
        else if (ClientConfig.enableMaxMemoryWarning.get() && currentMem > ClientConfig.warnMaxMemoryAmountInGB.get().floatValue())
            ClientUtils.startUIProcess(Message.Content.WantsLessMemory);
    }

    @SubscribeEvent
    public static void clientInit(final FMLClientSetupEvent event) {
        final Minecraft mcInstance = event.getMinecraftSupplier().get();

        // Java arch requirement and warning
        final boolean isJava64bit = mcInstance.isJava64bit();
        itlt.LOGGER.debug("isJava64bit: " + isJava64bit);
        if (!isJava64bit) {
            if (ClientConfig.enable64bitRequirement.get()) ClientUtils.startUIProcess(Message.Content.NeedsJava64bit);
            else if (ClientConfig.enable64bitWarning.get()) ClientUtils.startUIProcess(Message.Content.WantsJava64bit);
        }

        // Custom server list entries
        if (ClientConfig.enableCustomServerListEntries.get()) {
            final File itltDir = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "itlt").toFile();

            if (!itltDir.exists()) {
                itlt.LOGGER.warn("itlt folder in the config folder is missing.");
                if (itltDir.mkdir()) itlt.LOGGER.info("The folder has been successfully created for you.");
                else itlt.LOGGER.warn("Please create a folder named \"itlt\" (case sensitive) in the config folder.");
                return;
            }

            final Path customServersJsonPath = Paths.get(itltDir.getAbsolutePath(), "servers.json");
            final File customServersJsonFile = customServersJsonPath.toFile();
            if (!customServersJsonFile.exists() || customServersJsonFile.isDirectory()) {
                itlt.LOGGER.warn("enableCustomServerListEntries is true but servers.json is missing or invalid.");
                return;
            }

            try {
                // read the file
                final String customServersJson = ClientUtils.readString(customServersJsonPath);

                // parse it into an array of CustomServerData classes
                final ClientUtils.CustomServerData[] featuredList = new Gson().fromJson(customServersJson, ClientUtils.CustomServerData[].class);
                if (featuredList != null) {
                    final ServerList serverList = new ServerList(mcInstance);

                    for (final ClientUtils.CustomServerData customServerEntry : featuredList) {
                        final ServerData serverToAdd = new ServerData(customServerEntry.name, customServerEntry.address, false);
                        if (customServerEntry.forceResourcePack)
                            serverToAdd.setResourceMode(ServerData.ServerResourceMode.ENABLED);

                        if (!ClientUtils.alreadyInServerList(serverToAdd, serverList)) {
                            itlt.LOGGER.info("Adding custom server entry");
                            serverList.addServerData(serverToAdd);
                            serverList.saveServerList();
                        }
                    }
                }
            } catch (final IOException e) {
                itlt.LOGGER.error("Unable to read the contents of " + customServersJsonPath);
                e.printStackTrace();
            }
        }
    }
}

package xyz.nextalone.nnngram.helpers;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import xyz.nextalone.nnngram.config.ConfigManager;
import xyz.nextalone.nnngram.utils.Defines;
import xyz.nextalone.nnngram.utils.Log;

public class UpdateHelper {

    static final int MAX_READ_COUNT = 50;
    static final long STABLE_METADATA_CHANNEL_ID = -999;
    static final String STABLE_METADATA_CHANNEL_NAME = "NnngramMetaData";
    static final long PREVIEW_METADATA_CHANNEL_ID = -999;
    static final String PREVIEW_METADATA_CHANNEL_NAME = "PreviewMetaData";
    static final long STABLE_UPDATE_CHANNEL_APKS_ID = -999;
    static final String STABLE_UPDATE_CHANNEL_APKS_NAME = "NnngramAPKs";
    static final long PREVIEW_UPDATE_CHANNEL_APKS_ID = -999;
    static final String PREVIEW_UPDATE_CHANNEL_APKS_NAME = "NnngramCI";

    static void retrieveUpdateMetadata(retrieveUpdateMetadataCallback callback) {
        long CHANNEL_METADATA_ID;
        String CHANNEL_METADATA_NAME;
        switch (ConfigManager.getIntOrDefault(Defines.updateChannel, -1)) {
            case Defines.stableChannel:
                CHANNEL_METADATA_ID = STABLE_METADATA_CHANNEL_ID;
                CHANNEL_METADATA_NAME = STABLE_METADATA_CHANNEL_NAME;
                break;

            case Defines.ciChannel:
                CHANNEL_METADATA_ID = PREVIEW_METADATA_CHANNEL_ID;
                CHANNEL_METADATA_NAME = PREVIEW_METADATA_CHANNEL_NAME;
                break;
            default:
                if (BuildConfig.VERSION_NAME.contains("preview")) {
                    CHANNEL_METADATA_ID = PREVIEW_METADATA_CHANNEL_ID;
                    CHANNEL_METADATA_NAME = PREVIEW_METADATA_CHANNEL_NAME;
                } else {
                    CHANNEL_METADATA_ID = STABLE_METADATA_CHANNEL_ID;
                    CHANNEL_METADATA_NAME = STABLE_METADATA_CHANNEL_NAME;
                }
                break;
        }

        final int localVersionCode = BuildConfig.VERSION_CODE;
        AccountInstance accountInstance = AccountInstance.getInstance(UserConfig.selectedAccount);
        TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
        req.peer = accountInstance.getMessagesController().getInputPeer(-CHANNEL_METADATA_ID);
        req.offset_id = 0;
        req.limit = MAX_READ_COUNT;
        Runnable sendReq = () -> accountInstance.getConnectionsManager()
            .sendRequest(req, (response, error) -> {
                if (error != null) {
                    Log.e("Error when retrieving update metadata from channel " + error);
                    callback.apply(null, true);
                    return;
                }
                TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                List<UpdateMetadata> metas = new ArrayList<>();
                for (TLRPC.Message message : res.messages) {
                    if (!(message instanceof TLRPC.TL_message)) {
                        Log.i("CheckUpdate: Not TL_message");
                        continue;
                    }
                    if (!message.message.startsWith("v")) {
                        Log.i("CheckUpdate: Not startsWith v");
                        continue;
                    }
                    String[] split = message.message.split(",");
                    if (split.length < 4) {
                        Log.i("CheckUpdate: split < 4");
                        continue;
                    }
                    UpdateMetadata metaData = new UpdateMetadata(message.id, split);
                    metas.add(metaData);
                }
                Collections.sort(metas,
                    (o1, o2) -> o2.versionCode - o1.versionCode); // versionCode Desc
                UpdateMetadata found = null;
                int releaseChannel = ConfigManager.getIntOrDefault(Defines.updateChannel,
                    Defines.stableChannel);
                for (UpdateMetadata metaData : metas) {
                    if (metaData.versionCode <= localVersionCode) {
                        Log.i("versionCode <= localVersionCode , ignore.");
                        break;
                    }
                    if (releaseChannel < Defines.ciChannel && metaData.versionName.contains(
                        "preview")) {
                        Log.i("Current Release Channel: " + ConfigManager.getIntOrDefault(Defines.updateChannel, -1));
                        Log.i("Found preview metaData, but ignore.");
                        continue;
                    }
                    found = metaData;
                    break;
                }
                if (found != null) {
                    for (TLRPC.Message message : res.messages) {
                        if (!(message instanceof TLRPC.TL_message)) {
                            continue;
                        }
                        if (message.id == found.UpdateLogMessageID) {
                            found.updateLog = message.message;
                            found.updateLogEntities = message.entities;
                            break;
                        }
                    }
                }
                if (found == null) {
                    Log.d("Cannot find Update Metadata");
                    callback.apply(null, false);
                    return;
                }
                Log.i(
                    "Found Update Metadata " + found.versionName + " " + found.versionCode);
                callback.apply(found, false);
            });
        if (req.peer.access_hash != 0) {
            sendReq.run();
        } else {
            TLRPC.TL_contacts_resolveUsername resolve = new TLRPC.TL_contacts_resolveUsername();
            resolve.username = CHANNEL_METADATA_NAME;
            accountInstance.getConnectionsManager().sendRequest(resolve, (response1, error1) -> {
                if (error1 != null) {
                    Log.e("Error when checking update, unable to resolve metadata channel "
                        + error1.text);
                    callback.apply(null, true);
                    return;
                }
                if (!(response1 instanceof TLRPC.TL_contacts_resolvedPeer)) {
                    Log.e(
                        "Error when checking update, unable to resolve metadata channel, unexpected responseType "
                            + response1.getClass().getName());
                    callback.apply(null, true);
                    return;
                }
                TLRPC.TL_contacts_resolvedPeer resolvedPeer = (TLRPC.TL_contacts_resolvedPeer) response1;
                accountInstance.getMessagesController().putUsers(resolvedPeer.users, false);
                accountInstance.getMessagesController().putChats(resolvedPeer.chats, false);
                accountInstance.getMessagesStorage()
                    .putUsersAndChats(resolvedPeer.users, resolvedPeer.chats, false, true);
                if ((resolvedPeer.chats == null || resolvedPeer.chats.size() == 0)) {
                    Log.e(
                        "Error when checking update, unable to resolve metadata channel, unexpected resolvedChat ");
                    callback.apply(null, true);
                    return;
                }
                req.peer = new TLRPC.TL_inputPeerChannel();
                req.peer.channel_id = resolvedPeer.chats.get(0).id;
                req.peer.access_hash = resolvedPeer.chats.get(0).access_hash;
                sendReq.run();
            });
        }
    }

    public static void checkUpdate(checkUpdateCallback callback) {
        if (BuildConfig.isPlay) {
            return;
        }

        long CHANNEL_APKS_ID;
        String CHANNEL_APKS_NAME;
        switch (ConfigManager.getIntOrDefault(Defines.updateChannel, -1)) {
            case Defines.stableChannel:
                CHANNEL_APKS_ID = STABLE_UPDATE_CHANNEL_APKS_ID;
                CHANNEL_APKS_NAME = STABLE_UPDATE_CHANNEL_APKS_NAME;
                break;

            case Defines.ciChannel:
                CHANNEL_APKS_ID = PREVIEW_UPDATE_CHANNEL_APKS_ID;
                CHANNEL_APKS_NAME = PREVIEW_UPDATE_CHANNEL_APKS_NAME;
                break;
            default:
                if (BuildConfig.VERSION_NAME.contains("preview")) {
                    CHANNEL_APKS_ID = PREVIEW_UPDATE_CHANNEL_APKS_ID;
                    CHANNEL_APKS_NAME = PREVIEW_UPDATE_CHANNEL_APKS_NAME;
                } else {
                    CHANNEL_APKS_ID = STABLE_UPDATE_CHANNEL_APKS_ID;
                    CHANNEL_APKS_NAME = STABLE_UPDATE_CHANNEL_APKS_NAME;
                }
                break;
        }
        AccountInstance accountInstance = AccountInstance.getInstance(
            UserConfig.selectedAccount);
        retrieveUpdateMetadata((metadata, err) -> {
            if (metadata == null) {
                Log.d("checkUpdate: metadata is null");
                callback.apply(null, err);
                return;
            }
            TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
            req.peer = accountInstance.getMessagesController().getInputPeer(-CHANNEL_APKS_ID);
            req.min_id = metadata.apkChannelMessageID;
            req.limit = MAX_READ_COUNT;

            Runnable sendReq = () -> accountInstance.getConnectionsManager()
                .sendRequest(req, (response, error) -> {
                    try {
                        if (error != null) {
                            Log.e("Error when getting update document " + error.text);
                            callback.apply(null, true);
                            return;
                        }
                        TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                        Log.d("Retrieve update messages, size:" + res.messages.size());
                        final String target = BuildConfig.FLAVOR + ".apk";
                        Log.d("target:" + target);
                        for (int i = 0; i < res.messages.size(); i++) {
                            if (res.messages.get(i).media == null) {
                                Log.i("CheckUpdate: res.messages.get(i).media == null");
                                continue;
                            }

                            TLRPC.Document apkDocument = res.messages.get(i).media.document;
                            String fileName = apkDocument.attributes.size() == 0 ? ""
                                : apkDocument.attributes.get(0).file_name;
                            Log.d("file_name： " + apkDocument.attributes.get(0).file_name);
                            if (!(fileName.contains(BuildConfig.FLAVOR) && fileName.contains(
                                metadata.versionName))) {
                                continue;
                            }
                            TLRPC.TL_help_appUpdate update = new TLRPC.TL_help_appUpdate();
                            update.version = metadata.versionName;
                            update.document = apkDocument;
                            update.can_not_skip = false;
                            update.flags |= 2;
                            if (metadata.updateLog != null) {
                                update.text = metadata.updateLog;
                                update.entities = metadata.updateLogEntities;
                            }
                            callback.apply(update, false);
                            return;
                        }
                        callback.apply(null, false);
                    } catch (Exception e) {
                        Log.e(e);
                    }
                });
            if (req.peer.access_hash != 0) {
                sendReq.run();
            } else {
                TLRPC.TL_contacts_resolveUsername resolve = new TLRPC.TL_contacts_resolveUsername();
                resolve.username = CHANNEL_APKS_NAME;
                accountInstance.getConnectionsManager()
                    .sendRequest(resolve, (response1, error1) -> {
                        if (error1 != null) {
                            Log.e(
                                "Error when checking update, unable to resolve metadata channel "
                                    + error1);
                            callback.apply(null, true);
                            return;
                        }
                        if (!(response1 instanceof TLRPC.TL_contacts_resolvedPeer)) {
                            Log.e(
                                "Error when checking update, unable to resolve metadata channel, unexpected responseType "
                                    + response1.getClass().getName());
                            callback.apply(null, true);
                            return;
                        }
                        TLRPC.TL_contacts_resolvedPeer resolvedPeer = (TLRPC.TL_contacts_resolvedPeer) response1;
                        accountInstance.getMessagesController()
                            .putUsers(resolvedPeer.users, false);
                        accountInstance.getMessagesController()
                            .putChats(resolvedPeer.chats, false);
                        accountInstance.getMessagesStorage()
                            .putUsersAndChats(resolvedPeer.users, resolvedPeer.chats, false,
                                true);
                        if ((resolvedPeer.chats == null || resolvedPeer.chats.size() == 0)) {
                            Log.e(
                                "Error when checking update, unable to resolve metadata channel, unexpected resolvedChat ");
                            callback.apply(null, true);
                            return;
                        }
                        req.peer = new TLRPC.TL_inputPeerChannel();
                        req.peer.channel_id = resolvedPeer.chats.get(0).id;
                        req.peer.access_hash = resolvedPeer.chats.get(0).access_hash;
                        sendReq.run();
                    });
            }
        });

    }

    public interface retrieveUpdateMetadataCallback {

        void apply(UpdateMetadata metadata, boolean error);
    }

    public interface checkUpdateCallback {

        void apply(TLRPC.TL_help_appUpdate resp, boolean error);
    }

    static class UpdateMetadata {

        int messageID;
        String versionName;
        int versionCode;
        int apkChannelMessageID;
        int UpdateLogMessageID;
        String updateLog = null;
        ArrayList<TLRPC.MessageEntity> updateLogEntities = null;

        UpdateMetadata(int messageID, String[] split) {
            this.messageID = messageID;
            versionName = split[0];
            versionCode = Integer.parseInt(split[1]);
            apkChannelMessageID = Integer.parseInt(split[2]);
            UpdateLogMessageID = Integer.parseInt(split[3]);
        }
    }

}

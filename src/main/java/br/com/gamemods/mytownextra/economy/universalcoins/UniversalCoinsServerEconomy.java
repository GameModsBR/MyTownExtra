package br.com.gamemods.mytownextra.economy.universalcoins;

import br.com.gamemods.universalcoinsserver.UniversalCoinsServer;
import br.com.gamemods.universalcoinsserver.api.ScanResult;
import br.com.gamemods.universalcoinsserver.api.UniversalCoinsServerAPI;
import br.com.gamemods.universalcoinsserver.datastore.*;
import br.com.gamemods.universalcoinsserver.item.ItemCard;
import myessentials.economy.IEconManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StatCollector;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public class UniversalCoinsServerEconomy implements IEconManager, Operator
{
    private UUID playerId;

    private EntityPlayer getOnlinePlayer()
    {
        //noinspection unchecked
        List<EntityPlayer> players = (List<EntityPlayer>) MinecraftServer.getServer().getConfigurationManager().playerEntityList;
        for(EntityPlayer player: players)
            if(player.getPersistentID().equals(playerId))
                return player;

        return null;
    }

    @Override
    public void setPlayer(UUID uuid)
    {
        this.playerId = uuid;
    }

    @Override
    public void addToWallet(int amountToAdd)
    {
        try
        {
            PlayerData playerData = UniversalCoinsServer.cardDb.getPlayerData(playerId);

            AccountAddress account;

            if(playerData.hasPrimaryAccount())
                account = playerData.getPrimaryAccount();
            else
            {
                EntityPlayer onlinePlayer = getOnlinePlayer();
                if(onlinePlayer != null)
                {
                    UniversalCoinsServerAPI.giveCoins(onlinePlayer, amountToAdd);
                    return;
                }

                account = UniversalCoinsServer.cardDb.createPrimaryAccount(playerId, "UniversalCoins");
                account = UniversalCoinsServer.cardDb.renamePrimaryAccount(account, account.getNumber().toString());
            }

            Transaction transaction = new Transaction(null, Transaction.Operation.DEPOSIT_TO_ACCOUNT_BY_API, this, null,
                    new Transaction.CardCoinSource(account, amountToAdd), null);

            int change = UniversalCoinsServer.cardDb.depositToAccount(playerData.getPrimaryAccount(), amountToAdd, transaction);
            if(change > 0)
            {
                EntityPlayer onlinePlayer = getOnlinePlayer();
                if(onlinePlayer != null)
                    UniversalCoinsServerAPI.giveCoins(onlinePlayer, change);
            }
        } catch (DataBaseException e)
        {
            e.printStackTrace();

            EntityPlayer onlinePlayer = getOnlinePlayer();
            if(onlinePlayer != null)
                UniversalCoinsServerAPI.giveCoins(onlinePlayer, amountToAdd);
            else
                throw new RuntimeException(e);
        }
    }

    @Override
    public int getWallet()
    {
        EntityPlayer onlinePlayer = getOnlinePlayer();
        if(onlinePlayer != null)
            return UniversalCoinsServerAPI.scanCoins(onlinePlayer.inventory).getCoins();

        try
        {
            PlayerData playerData = UniversalCoinsServer.cardDb.getPlayerData(playerId);
            if(playerData.hasPrimaryAccount())
                return UniversalCoinsServer.cardDb.getAccountBalance(playerData.getPrimaryAccount());
            return 0;
        } catch (DataBaseException e)
        {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public boolean removeFromWallet(int amountToSubtract)
    {
        EntityPlayer onlinePlayer = getOnlinePlayer();
        if(onlinePlayer != null)
        {
            for(ItemStack stack: onlinePlayer.inventory.mainInventory)
            {
                if(stack != null && stack.getItem() instanceof ItemCard)
                {
                    if(UniversalCoinsServerAPI.canCardBeUsedBy(stack, onlinePlayer))
                    {
                        AccountAddress address = UniversalCoinsServerAPI.getAddress(stack);
                        if(address == null) continue;
                        try
                        {
                            int balance = UniversalCoinsServer.cardDb.getAccountBalance(address);
                            if(balance >= amountToSubtract)
                            {
                                Transaction transaction = new Transaction(null, Transaction.Operation.WITHDRAW_FROM_ACCOUNT_BY_API, this, null,
                                        new Transaction.CardCoinSource(stack, -amountToSubtract), null);

                                UniversalCoinsServer.cardDb.takeFromAccount(address, amountToSubtract, transaction);
                                return true;
                            }
                        }
                        catch (DataStoreException e)
                        {
                            e.printStackTrace();
                        }
                        catch (AccountNotFoundException ignored)
                        {}
                        catch (OutOfCoinsException ignored)
                        {}

                    }
                }
            }

            ScanResult scanResult = UniversalCoinsServerAPI.scanCoins(onlinePlayer.inventory);
            if(scanResult.getCoins() < amountToSubtract)
                return false;

            UniversalCoinsServerAPI.takeCoinsReturningChange(scanResult, amountToSubtract, onlinePlayer);
            return true;
        }

        try
        {
            PlayerData playerData = UniversalCoinsServer.cardDb.getPlayerData(playerId);
            if(!playerData.hasPrimaryAccount())
                return false;

            int accountBalance = UniversalCoinsServer.cardDb.getAccountBalance(playerData.getPrimaryAccount());
            if(accountBalance < amountToSubtract)
                return false;

            Transaction transaction = new Transaction(null, Transaction.Operation.WITHDRAW_FROM_ACCOUNT_BY_API, this, null,
                    new Transaction.CardCoinSource(playerData.getPrimaryAccount(), -amountToSubtract), null);

            UniversalCoinsServer.cardDb.takeFromAccount(playerData.getPrimaryAccount(), amountToSubtract, transaction);
            return true;
        } catch (DataBaseException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void setWallet(int setAmount, EntityPlayer player)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String currency(int amount)
    {
        return amount+" " + StatCollector.translateToLocal("item.itemCoin.name");
    }

    @Override
    public String getMoneyString()
    {
        return StatCollector.translateToLocal("item.itemCoin.name");
    }

    @Override
    public void save()
    {

    }

    @Override
    public Map<String, Integer> getItemTables()
    {
        throw new UnsupportedOperationException();
    }
}

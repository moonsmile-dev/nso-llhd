package real;

import io.Message;
import java.io.IOException;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import org.json.simple.JSONArray;
import server.SQLManager;
import server.Server;
import server.util;

/**
 *
 * @author ghost
 */
public class ClanManager {
    
    public int id;
    public String name = "";
    public int exp = 0;
    public int level = 1;
    public int itemLevel = 0;
    public int coin = 1000000;
    public String reg_date = "";
    public String log = "";
    public String alert = "";
    public byte use_card = 4;
    public byte openDun = 3;
    public byte debt = 0;
    public ArrayList<ClanMember> members = new ArrayList<>();
    public ArrayList<Item> items = new ArrayList<>();
    public String week = "";
    
    public final static ArrayList<ClanManager> entrys = new ArrayList<>();
    
    public static ClanManager getClanName(String name) {
        for (int i = 0; i < entrys.size(); i++) {
            if (entrys.get(i).name.equals(name))
                return entrys.get(i);
        }
        return null;
    }
    
    public static ClanManager getClanId(int id) {
        for (int i = 0; i < entrys.size(); i++) {
            if (entrys.get(i).id == id)
                return entrys.get(i);
        }
        return null;
    }
    
    public void updateCoin(int coin) {
        this.coin += coin;
        if (coin < 0 && this.coin < 0) {
            this.debt++;
            if (this.debt > 3) {
                dissolution();
            }
        } else if (this.coin >= 0) {
            this.debt = 0;
        }
    }
    
    public String getmain_name() {
        for (short i = 0; i < members.size(); i++) {
            if (members.get(i).typeclan == 4)
                return members.get(i).cName;
        }
        return "";
    }
    
    public String getassist_name() {
        for (short i = 0; i < members.size(); i++) {
            if (members.get(i).typeclan == 3)
                return members.get(i).cName;
        }
        return "";
    }
    
    public int numElder() {
        int elder = 0;
        for (short i = 0; i < members.size(); i++) {
            if (members.get(i).typeclan == 2)
                elder++;
        }
        return elder;
    }
    
    public ClanMember getMem(int id) {
        for (short i = 0; i < members.size(); i++) {
            if (members.get(i).charID == id)
                return members.get(i);
        }
        return null;
    }
    
    public ClanMember getMem(String name) {
        for (short i = 0; i < members.size(); i++) {
            if (members.get(i).cName.equals(name))
                return members.get(i);
        }
        return null;
    }
    
    public int getMemMax() {
        return 45 + (level * 5);
    }
    
    public int getexpNext() {
        int expNext = 2000;
        for (int i = 1; i < level; i++) {
            if (i == 1) {
                expNext = 3720;
            } else {
                if (i < 10)
                    expNext = ((expNext/i)+310)*(i+1);
                else if (i < 20)
                    expNext = ((expNext/i)+620)*(i+1);
                else
                    expNext = ((expNext/i)+930)*(i+1);
            }
        }
        return expNext;
    }
    
    public int getfreeCoin() {
        return 30000 + (members.size()*5000);
    }
    
    private int getCoinOpen() {
        if (itemLevel == 0)
            return 1000000;
        if (itemLevel == 1)
            return 5000000;
        if (itemLevel == 2)
            return 10000000;
        if (itemLevel == 3)
            return 20000000;
        if (itemLevel == 4)
            return 30000000;
        else
            return 0;
    }
    
    public int getCoinUp() {
        int coinUp = 500000;
        for (int i = 1; i < level; i++) {
            if (i < 10)
                coinUp += 100000;
            else if (i < 20)
                coinUp += 200000;
            else
                coinUp += 300000;
        }
        return coinUp;
    }
    
    public void sendMessage(Message m) {
        for (short i = 0; i < members.size(); i++) {
            Char n = PlayerManager.getInstance().getNinja(members.get(i).cName);
            if (n != null)
                n.p.conn.sendMessage(m);
        }
    }
    
    public void payfeesClan() {
        this.writeLog("", 4, getfreeCoin(), util.toDateString(Date.from(Instant.now())));
        this.updateCoin(-this.getfreeCoin());
        for (short i = 0; i < members.size(); i++) {
            members.get(i).pointClanWeek = 0;
        }
        this.week = util.toDateString(Date.from(Instant.now()));
    }
    
    public void upExp(int exp) {
        this.exp += exp;
    }
    
    public void addItem(Item it) {
        for (byte i = 0; i < items.size(); i++) {
            Item it2 = items.get(i);
            if (it2.id == it.id) {
                it2.quantity += it.quantity;
                return;
            }
        }
        items.add(it);
    }
    
    public void removeItem(int id, int quantity) {
        for (byte i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            if (it.id == id) {
                it.quantity -= quantity;
                if (it.quantity <= 0)
                    items.remove(it);
                return;
            }
        }
    }
    
    public void chat(Player p, Message m) throws IOException {
        String text = m.reader().readUTF();
        m.cleanup();
        m = new Message(-19);
        m.writer().writeUTF(p.c.name);
        m.writer().writeUTF(text);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }
    
    public void changeClanType(Player p, Message m) throws IOException {
        String cName = m.reader().readUTF();
        byte typeclan = m.reader().readByte();
        ClanMember mem = this.getMem(cName);
        if (mem == null || p.c.clan.typeclan != 4 || mem.charID == p.c.id)
            return;
        Char n = PlayerManager.getInstance().getNinja(mem.cName);
        if (typeclan == 0 && mem.typeclan > 1) {
            if (n != null) {
                n.p.setTypeClan(typeclan);
            }
            mem.typeclan = typeclan;
            requestClanMember(p);
            m = new Message(-24);
            m.writer().writeUTF(p.c.name+" ???? b??? b??i ch???c");
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } else if (typeclan == 2) {
            if (this.numElder() >= 5) {
                p.conn.sendMessageLog("???? c?? ????? tr?????ng l??o");
                return;
            }
            if (n != null) {
                n.p.setTypeClan(typeclan);
            }
            mem.typeclan = typeclan;
            requestClanMember(p);
            m = new Message(-24);
            m.writer().writeUTF(p.c.name+" ???? ???????c b??? nhi???m l??m tr?????ng l??o");
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } else if (typeclan == 3) {
            if (this.getassist_name().length() > 0) {
                p.conn.sendMessageLog("???? c?? t???c ph?? r???i");
                return;
            }
            if (n != null) {
                n.p.setTypeClan(typeclan);
            }
            mem.typeclan = typeclan;
            requestClanMember(p);
            m = new Message(-24);
            m.writer().writeUTF(p.c.name+" ???? ???????c b??? nhi???m l??m t???c ph??");
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        }
    }
    
    public void openItemLevel(Player p) throws IOException {
        if (p.c.clan.typeclan == 4 || p.c.clan.typeclan == 3) {
            int coinDown = getCoinOpen();
            int lvopen = 5*(itemLevel+1);
            if (lvopen > level) {
                p.conn.sendMessageLog("Gia t???c ch??a ?????t c???p "+lvopen);
            } else if (coinDown > coin) {
                p.conn.sendMessageLog("Ng??n s??ch kh??ng ????? ????? khai m??? v???t ph???m");
            } else if (itemLevel == 5) {
                p.conn.sendMessageLog("Khai m??? ???? t???i ??a");
            } else {
                this.updateCoin(-coinDown);
                this.itemLevel++;
                Message m = new Message(-28);
                m.writer().writeByte(-62);
                m.writer().writeByte(this.itemLevel);
                m.writer().flush();
                p.conn.sendMessage(m);
                m.cleanup();
                this.requestClanInfo(p);
                m = new Message(-24);
                m.writer().writeUTF(p.c.name+" ???? khai m??? v???t gia t???c ng??n s??ch gi???m "+coin+" xu");
                m.writer().flush();
                this.sendMessage(m);
                m.cleanup();
            }
        }
    }
    
    public void sendClanItem(Player p, Message m) throws IOException {
        byte index = m.reader().readByte();
        String cName = m.reader().readUTF();
        m.cleanup();
        ClanMember mem = this.getMem(cName);
        if (mem == null || p.c.clan.typeclan < 3 || index < 0 || index >= items.size())
            return;
        Char n = PlayerManager.getInstance().getNinja(mem.cName);
        if (n == null) {
            p.sendAddchatYellow("Th??nh vi??n ???? offline");
        } else if (n.getBagNull() == 0) {
            p.sendAddchatYellow("H??nh trang th??nh vi??n ???? ?????y");
        } else {
            Item item = items.get(index).clone();
            item.expires = System.currentTimeMillis()+item.expires;
            item.isLock = true;
            item.quantity = 1;
            this.removeItem(item.id, 1);
            n.addItemBag(false, item);
            requestClanItem(p);
        }
    }
    
    public void setAlert(Player p, Message m) throws IOException {
        String newalert = m.reader().readUTF();
        m.cleanup();
        if (p.c.clan.typeclan == 4 || p.c.clan.typeclan == 3) {
            if (newalert.length() > 30) {
                p.conn.sendMessageLog("Chi???u d??i kh??ng qu?? 30 k?? t???");
                return;
            }
            if (newalert.isEmpty()) {
                this.alert = "";
            } else {
                this.alert = ("Ghi ch?? c???a "+p.c.name+"\n"+newalert);
            }
            m = new Message(-28);
            m.writer().writeByte(-95);
            m.writer().writeUTF(this.alert);
            m.writer().flush();
            p.conn.sendMessage(m);
            m.cleanup();
        }
    }
    
    public void moveOutClan(Player p, Message m) throws IOException {
        String cName = m.reader().readUTF();
        m.cleanup();
        ClanMember mem = this.getMem(cName);
        if (mem == null || p.c.clan.typeclan < 3 || mem.typeclan == 4 || mem.charID == p.c.id)
            return;
        Char n = PlayerManager.getInstance().getNinja(mem.cName);
        int coinDown = 10000;
        if (mem.typeclan == 3) {
            coinDown = 100000;
        } else if (mem.typeclan == 2) {
            coinDown = 50000;
        } else if (mem.typeclan == 1) {
            coinDown = 20000;
        }
        if (n != null) {
            n.clan.clanName = "";
            n.clan.pointClanWeek = 0;
            n.p.setTypeClan(-1);
        }
        writeLog(mem.cName, 1, coinDown, util.toDateString(Date.from(Instant.now())));
        m = new Message(-24);
        m.writer().writeUTF(mem.cName+" ???? b??? tr???c su???t kh???i gia t???c");
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
        members.remove(mem);
        this.updateCoin(-coinDown);
        this.requestClanMember(p);
    }
    
    public void OutClan(Player p) throws IOException {
        ClanMember mem = this.getMem(p.c.id);
        if (p.c.clan.typeclan == 4 || mem ==  null)
            return;
        int coinDown = 10000;
        if (p.c.clan.typeclan == 3) {
            coinDown = 100000;
        } else if (p.c.clan.typeclan == 2) {
            coinDown = 50000;
        } else if (p.c.clan.typeclan == 1) {
            coinDown = 20000;
        }
        if (coinDown > p.c.xu) {
            p.conn.sendMessageLog("B???n kh??ng c?? ????? xu");
            return;
        }
        p.c.clan.clanName = "";
        p.c.clan.pointClanWeek = 0;
        p.setTypeClan(-1);
        p.c.upxu(-coinDown);
        Message m = new Message(-28);
        m.writer().writeByte(-90);
        m.writer().writeInt(p.c.xu);
        m.writer().flush();
        p.conn.sendMessage(m);
        m = new Message(-24);
        m.writer().writeUTF(mem.cName+" ???? r???i kh???i gia t???c tr??? -"+coinDown+" xu");
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
        members.remove(mem);
    }

    
    public void clanUpLevel(Player p) throws IOException {
        if (p.c.clan.typeclan == 4 || p.c.clan.typeclan == 3) {
            int coinDown = getCoinUp();
            int expDown = getexpNext();
            if (this.getexpNext() > exp) {
                p.conn.sendMessageLog("Kinh nghi???m ch??a ?????");
            } else if (this.getCoinUp() > coin) {
                p.conn.sendMessageLog("Ng??n s??ch kh??ng ?????");
            } else {
                writeLog(p.c.name, 5, coinDown, util.toDateString(Date.from(Instant.now())));
                this.updateCoin(-getCoinUp());
                this.upExp(-expDown);
                this.level++;
                Message m = new Message(-24);
                m.writer().writeUTF(p.c.name+" ???? n??ng c???p gia t???c ng??n s??ch gi???m "+coinDown+" xu");
                m.writer().flush();
                this.sendMessage(m);
                m.cleanup();
                this.requestClanInfo(p);
            }
        }
    }
    
    public void inputCoinClan(Player p, Message m) throws IOException {
        int inputcoin = m.reader().readInt();
        m.cleanup();
        if (inputcoin > 0) {
            if (inputcoin > p.c.xu) {
                p.conn.sendMessageLog("B???n kh??ng c?? ????? xu.");
                return;
            }
            if ((long)inputcoin + coin > 2000000000) {
                p.conn.sendMessageLog("Ch??? c??n c?? th??? ????ng g??p th??m "+(coin-inputcoin));
                return;
            }
            this.writeLog(p.c.name, 2, inputcoin, util.toDateString(Date.from(Instant.now())));
            this.updateCoin(inputcoin);
            p.c.upxu(-inputcoin);
            m = new Message(-28);
            m.writer().writeByte(-90);
            m.writer().writeInt(p.c.xu);
            m.writer().flush();
            p.conn.sendMessage(m);
            m.cleanup();
            m = new Message(-24);
            m.writer().writeUTF(p.c.name+" ???? ????ng g??p "+inputcoin+" xu v??o gia t???c ng??n s??ch t??ng "+coin+" xu");
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        }
    }
    
    public void writeLog(String name, int num, int number, String date) {
        String[] array = log.split("\n");
        log = name+","+num+","+number+","+date+"\n";
        for (int i = 0; i < array.length; i++) {
            if (i == 10)
                break;
            log += array[i]+"\n";
        }
    }
    
    public void LogClan(Player p) throws IOException {
        Message m = new Message(-28);
        m.writer().writeByte(-114);
        m.writer().writeUTF(log);
        m.writer().flush();
        p.conn.sendMessage(m);
        m.cleanup();
    }
    
    public void requestClanInfo(Player p) throws IOException {
        Message m = new Message(-28);
        m.writer().writeByte(-113);
        m.writer().writeUTF(name);
        m.writer().writeUTF(getmain_name());
        m.writer().writeUTF(getassist_name());
        m.writer().writeShort(members.size());
        m.writer().writeByte(openDun);
        m.writer().writeByte(level);
        m.writer().writeInt(exp);
        m.writer().writeInt(getexpNext());
        m.writer().writeInt(coin);
        m.writer().writeInt(getfreeCoin());
        m.writer().writeInt(getCoinUp());
        m.writer().writeUTF(reg_date);
        m.writer().writeUTF(alert);
        m.writer().writeInt(use_card);
        m.writer().writeByte(itemLevel);
        m.writer().flush();
        p.conn.sendMessage(m);
        m.cleanup();
    }
    
    public void requestClanMember(Player p) throws IOException {
        Message m = new Message(-28);
        m.writer().writeByte(-112);
        m.writer().writeShort(members.size());
        for (short i = 0; i < members.size(); i++) {
            Char n = PlayerManager.getInstance().getNinja(members.get(i).cName);
            m.writer().writeByte(members.get(i).nClass);
            m.writer().writeByte(members.get(i).clevel);
            m.writer().writeByte(members.get(i).typeclan);
            m.writer().writeUTF(members.get(i).cName);
            m.writer().writeInt(members.get(i).pointClan);
            m.writer().writeBoolean(n!=null);
        }
        for (short i = 0; i < members.size(); i++) {
            m.writer().writeInt(members.get(i).pointClanWeek);
        }
        m.writer().flush();
        p.conn.sendMessage(m);
        m.cleanup();
    }
    
    public void requestClanItem(Player p) throws IOException {
        Message m = new Message(-28);
        m.writer().writeByte(-111);
        m.writer().writeByte(items.size());
        for (byte i = 0; i < items.size(); i++) {
            m.writer().writeShort(items.get(i).quantity);
            m.writer().writeShort(items.get(i).id);
        }
        m.writer().writeByte(0);
        m.writer().flush();
        p.conn.sendMessage(m);
        m.cleanup();
    }
    
    public void flush() {
        try {
            synchronized (Server.LOCK_MYSQL) {
                JSONArray jarr = new JSONArray();
                String sqlSET = "`exp`="+exp+",`level`="+level+",`itemLevel`="+itemLevel+",`coin`="+coin+",`log`='"+log+"',`alert`='"+alert+"',`use_card`="+use_card+",`openDun`="+openDun+",`debt`="+debt+"";
                for (int i = 0; i < members.size(); i++) {
                    ClanMember mem = members.get(i);
                    JSONArray jarr2 = new JSONArray();
                    jarr2.add(mem.charID);
                    jarr2.add(mem.cName);
                    jarr2.add(mem.clanName);
                    jarr2.add(mem.typeclan);
                    jarr2.add(mem.clevel);
                    jarr2.add(mem.nClass);
                    jarr2.add(mem.pointClan);
                    jarr2.add(mem.pointClanWeek);
                    jarr.add(jarr2);
                }
                sqlSET += ",`members`='"+jarr.toJSONString()+"'";
                jarr.clear();
                for (short i = 0; i < items.size(); i++) {
                    Item item = items.get(i);
                    jarr.add(ItemData.ObjectItem(item, i));
                }
                sqlSET += ",`items`='"+jarr.toJSONString()+"'";
                jarr.clear();
                sqlSET += ",`week`='"+week+"'";
                SQLManager.stat.executeUpdate("UPDATE `clan` SET "+sqlSET+" WHERE `id`="+id+" LIMIT 1;");
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void createClan(Player p, String name) {
        name = name.toLowerCase();
        if (p.luong < 1500000) {
            p.conn.sendMessageLog("B???n c???n c?? 1.500.000 l?????ng ????? th??nh l???p gia t???c");
        }
        ClanManager clan = ClanManager.getClanName(name);
        if (!p.c.clan.clanName.isEmpty())
            return;
        if (!util.CheckString(name, "^[a-zA-Z0-9]+$") || name.length() < 5 || name.length() > 10) {
            p.conn.sendMessageLog("T??n gia t???c ch??? ?????ng ?? c??c k?? t??? a-z,0-9 v?? chi???u d??i t??? 5 ?????n 10 k?? t???");
        } else if (clan != null) {
            p.conn.sendMessageLog("T??n gia t???c ???? t???n t???i");
        } else {
            try {
                synchronized (Server.LOCK_MYSQL) {
                    clan = new ClanManager();
                    clan.name = name;
                    clan.reg_date = util.toDateString(Date.from(Instant.now()));
                    ClanMember mem = new ClanMember(name,(byte)4,p.c);
                    clan.members.add(mem);
                    p.c.clan = mem;
                    SQLManager.stat.executeUpdate("INSERT INTO clan(`name`,`reg_date`,`log`,`alert`,`members`) VALUES ('"+clan.name+"','"+clan.reg_date+"','"+clan.log+"','"+clan.alert+"','[]');");
                    ResultSet red = SQLManager.stat.executeQuery("SELECT `id` FROM `clan` WHERE `name`LIKE'"+clan.name+"' LIMIT 1;");
                    red.first();
                    clan.id = red.getInt("id");
                    clan.writeLog("", 0, clan.coin, util.toDateString(Date.from(Instant.now())));
                    clan.week = util.toDateString(Date.from(Instant.now()));
                    entrys.add(clan);
                    clan.flush();
                    p.c.flush();
                    p.upluong(-1500000);
                    Message m = new Message(-28);
                    m.writer().writeByte(-96);
                    m.writer().writeUTF(clan.name);
                    m.writer().writeInt(p.luong);
                    m.writer().flush();
                    p.conn.sendMessage(m);
                    m.cleanup();
                    p.setTypeClan(4);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void dissolution() {
        try {
            synchronized (entrys) {
                entrys.remove(this);
                Message m = new Message(-24);
                m.writer().writeUTF("Gia t???c "+name+" ???? b??? gi???i t??n");
                m.writer().flush();
                while (!members.isEmpty()) {
                    ClanMember mem = members.remove(0);
                    mem.typeclan = -1;
                    mem.clanName = "";
                    mem.pointClanWeek = 0;
                    Char n = PlayerManager.getInstance().getNinja(mem.cName);
                    if (n != null) {
                        n.p.setTypeClan(mem.typeclan);
                        n.p.conn.sendMessage(m);
                    }
                }
                m.cleanup();
                synchronized (Server.LOCK_MYSQL) {
                    SQLManager.stat.executeUpdate("DELETE FROM `clan` WHERE `id`="+id+" LIMIT 1;");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void close() {
        for (int i = 0; i < ClanManager.entrys.size(); i++) {
            ClanManager.entrys.get(i).flush();
        }
    }
}

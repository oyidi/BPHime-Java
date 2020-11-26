package com.windworkshop.bphime.object;

/**
 * 封包类型
 */
public enum PacketType {
    CLIENT_HEARTBEAT(2), COMMAND(5), JOIN_ROOM(7), SERVER_HEARTBEAT(8);
    int id;
    PacketType(int id){
        this.id = id;
    }

}
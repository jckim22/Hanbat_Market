package HM.Hanbat_Market.chat.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatResponseDto {
    private String senderUuid;
    private String receiverUuid;
    private String senderNickname;
    private String receiverNickname;
    private String lastChat;
    private String roomNum;
    private LocalDateTime createdAt;
    private int fcmOk;

    // Constructor, Getters, and Setters
    public ChatResponseDto(String senderUuid, String receiverUuid, String senderNickname, String receiverNickname, String lastChat, String roomNum, LocalDateTime createdAt, int fcmOk) {
        this.senderUuid = senderUuid;
        this.receiverUuid = receiverUuid;
        this.senderNickname = senderNickname;
        this.receiverNickname = receiverNickname;
        this.lastChat = lastChat;
        this.roomNum = roomNum;
        this.createdAt = createdAt;
        this.fcmOk = fcmOk;
    }

    // Getters and Setters 생략
}

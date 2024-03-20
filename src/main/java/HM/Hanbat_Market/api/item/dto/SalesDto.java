package HM.Hanbat_Market.api.item.dto;

import HM.Hanbat_Market.domain.entity.ArticleStatus;
import HM.Hanbat_Market.domain.entity.Item;
import HM.Hanbat_Market.domain.entity.Member;
import HM.Hanbat_Market.domain.entity.Trade;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SalesDto {
    private Long id;

    private String seller;

    private String title;

    private String description;

    private String tradingPlace;

    private ArticleStatus articleStatus;

    private String itemName;

    private Long price;

    private String thumbnailFilePath;

    private LocalDateTime createdAt;

    public SalesDto(Item item, String thumbnailFilePath){
        this.id = item.getArticle().getId();
        this.seller = item.getMember().getNickname();
        this.title = item.getArticle().getTitle();
        this.description = item.getArticle().getDescription();
        this.tradingPlace = item.getArticle().getTradingPlace();
        this.articleStatus = item.getArticle().getArticleStatus();
        this.itemName = item.getItemName();
        this.price = item.getPrice();
        this.thumbnailFilePath = thumbnailFilePath;
        this.createdAt = item.getArticle().getCreatedAt();
    }
}

package HM.Hanbat_Market.service.item;

import HM.Hanbat_Market.api.item.dto.*;
import HM.Hanbat_Market.domain.entity.*;
import HM.Hanbat_Market.repository.item.ItemRepository;
import HM.Hanbat_Market.service.member.MemberService;
import HM.Hanbat_Market.service.preemption.PreemptionItemService;
import HM.Hanbat_Market.service.trade.TradeService;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final PreemptionItemService preemptionItemService;
    private final TradeService tradeService;

    private final int THUMBNAIL_FILE_INDEX = 0;
    private final String FILE_URL = "https://cce1-39-119-25-167.ngrok-free.app/api/images/";

    public SalesHistoryResponseDto salesHistoryToDto(Member loginMember) {
        try {
            List<Item> itemByMember = itemRepository.findAllByMember(loginMember);
            List<SalesDto> salesDtos = new ArrayList<>();
            List<CompletedDto> completedDtos = new ArrayList<>();
            List<ReservedDto> reservedDtos = new ArrayList<>();
            for (Item item : itemByMember) {
                String filePath = getFullPath(item.getArticle().getImageFiles().get(THUMBNAIL_FILE_INDEX).getStoreFileName());
                ItemStatus itemStatus = item.getItemStatus();
                if (itemStatus == ItemStatus.SALE) {
                    salesDtos.add(new SalesDto(item, filePath));
                    continue;
                }
                if (itemStatus == ItemStatus.RESERVATION) {
                    reservedDtos.add(new ReservedDto(loginMember, item, filePath, getPreemptionSize(item)));
                } else if (itemStatus == ItemStatus.COMP) {
                    completedDtos.add(new CompletedDto(loginMember, item, filePath, getPreemptionSize(item)));
                }
            }
            List<PreemptionItem> preemptionItemByMember = preemptionItemService.findPreemptionItemByMember(loginMember);

            return new SalesHistoryResponseDto(preemptionItemByMember.size(), salesDtos, reservedDtos, completedDtos);
        } catch (NoResultException e) {
            List<PreemptionItem> preemptionItemByMember = preemptionItemService.findPreemptionItemByMember(loginMember);
            return new SalesHistoryResponseDto(preemptionItemByMember.size(), null, null, null);
        }
    }

    public PurchaseHistoryResponseDto purchaseHistoryToDto(Member loginMember) {
        try {
            List<Trade> completedByMember = tradeService.findCompletedByMember(loginMember);
            List<Trade> reservedByMember = tradeService.findReservedByMember(loginMember);
            List<PreemptionItem> preemptionItemByMember = preemptionItemService.findPreemptionItemByMember(loginMember);

            List<ReservedDto> reservedDtos = reservedByMember.stream()
                    .map(r -> new ReservedDto(loginMember, r.getItem(), getFilePathByTrade(r), getPreemptionSize(r.getItem())))
                    .collect(toList());

            List<CompletedDto> completedDtos = completedByMember.stream()
                    .map(c -> new CompletedDto(loginMember, c.getItem(), getFilePathByTrade(c), getPreemptionSize(c.getItem())))
                    .collect(toList());


            return new PurchaseHistoryResponseDto(preemptionItemByMember.size(), reservedDtos, completedDtos);
        } catch (NoResultException e) {
            List<PreemptionItem> preemptionItemByMember = preemptionItemService.findPreemptionItemByMember(loginMember);
            return new PurchaseHistoryResponseDto(preemptionItemByMember.size(), null, null);
        }
    }

    public PreemptionItemsResult preemptionItemsResultToDto(Member loginMember) {
        try {
            List<PreemptionItem> preemptionItemByMember = preemptionItemService.findPreemptionItemByMember(loginMember);

            List<PreemptionItemDto> preemptionItemDtos = preemptionItemByMember.stream()
                    .map(p -> new PreemptionItemDto(p.getItem().getMember(), p, getFilePathByPreemptionItem(p), getPreemptionSize(p.getItem())))
                    .collect(toList());

            return new PreemptionItemsResult(preemptionItemByMember.size(), preemptionItemDtos);
        } catch (NoResultException e) {
            List<PreemptionItem> preemptionItemByMember = preemptionItemService.findPreemptionItemByMember(loginMember);
            return new PreemptionItemsResult(preemptionItemByMember.size(), null);
        }
    }

    private String getFullPath(String filename) {
        return FILE_URL + filename;
    }

    private String getFilePathByTrade(Trade trade) {
        String filePath = getFullPath(trade.getItem().getArticle().getImageFiles().get(THUMBNAIL_FILE_INDEX).getStoreFileName());
        return filePath;
    }

    private String getFilePathByPreemptionItem(PreemptionItem preemptionItem) {
        String filePath = getFullPath(preemptionItem.getItem().getArticle().getImageFiles().get(THUMBNAIL_FILE_INDEX).getStoreFileName());
        return filePath;
    }

    private int getPreemptionSize(Item item) {
        return preemptionItemService.findPreemptionItemByItem(item).size();
    }
}

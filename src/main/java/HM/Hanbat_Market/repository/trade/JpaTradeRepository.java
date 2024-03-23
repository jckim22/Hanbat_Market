package HM.Hanbat_Market.repository.trade;

import HM.Hanbat_Market.domain.entity.Member;
import HM.Hanbat_Market.domain.entity.Trade;
import HM.Hanbat_Market.domain.entity.TradeStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaTradeRepository implements TradeRepository {

    private final EntityManager em;

    @Override
    public Long save(Trade trade) {
        em.persist(trade);
        return trade.getId();
    }

    @Override
    public Optional<Trade> findById(Long id) {
        return Optional.ofNullable(em.find(Trade.class, id));
    }

    //추후에 동적쿼리로 원하는 구매내역만을 볼 수 있는 기능 추가예정
    @Override
    public List<Trade> findAll() {
        return em.createQuery("select p from Trade p", Trade.class)
                .getResultList();
    }

    @Override
    public List<Trade> findCompleteByMember(Member member) {
        return em.createQuery("select t from Trade t join t.member m where " +
                        "t.tradeStatus = :tradeStatus and :purchaseMember != t.item.member.nickname and m.nickname = :purchaseMember", Trade.class)
                .setParameter("tradeStatus", TradeStatus.COMP)
                .setParameter("purchaseMember", member.getNickname())
                .getResultList();
    }

    @Override
    public List<Trade> findReservationByMember(Member member) {
        return em.createQuery("select t from Trade t join t.member m where " +
                        "t.tradeStatus = :tradeStatus and :purchaseMember != t.item.member.nickname and m.nickname = :purchaseMember", Trade.class)
                .setParameter("tradeStatus", TradeStatus.RESERVATION)
                .setParameter("purchaseMember", member.getNickname())
                .getResultList();
    }
}
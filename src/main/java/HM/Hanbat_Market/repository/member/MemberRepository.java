package HM.Hanbat_Market.repository.member;

import HM.Hanbat_Market.domain.entity.Member;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public interface MemberRepository {
    Member save(Member member);

    Optional<Member> findById(Long id);

    Optional<Member> findByNickName(String name);

    List<Member> findAll();

}
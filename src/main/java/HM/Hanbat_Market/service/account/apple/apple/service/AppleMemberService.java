package HM.Hanbat_Market.service.account.apple.apple.service;

import HM.Hanbat_Market.domain.entity.Member;
import HM.Hanbat_Market.domain.entity.Role;
import HM.Hanbat_Market.repository.member.MemberRepository;
import HM.Hanbat_Market.service.account.RefreshToken;
import HM.Hanbat_Market.service.account.RefreshTokenService;
import HM.Hanbat_Market.service.account.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AppleMemberService {
    private final MemberRepository memberRepository;
    private final JWTUtil jwtUtil;

    public Member saveOrUpdate(String email) {
        Member user = memberRepository.findByMail(email)
                // 구글 사용자 정보 업데이트(이미 가입된 사용자) => 업데이트
                .map(entity -> entity.update(email))
                // 가입되지 않은 사용자 => User 엔티티 생성
                .orElse(Member.createMember(email, email, "wncks4545!", "임시 닉네임" + email, Role.USER));

        return memberRepository.save(user);
    }

    public String getAccessToken(Member member) {
        String role = member.getRole().toString();
        String uuid = member.getUuid();

        String accessToken = jwtUtil.createAccessTokenJwt(uuid, member.getMail(), role);

        return accessToken;
    }

    public String getRefreshToken(Member member) {
        String role = member.getRole().toString();
        String uuid = member.getUuid();
        String mail = member.getMail();

        String refreshToken = jwtUtil.createRefreshTokenJwt(uuid, mail, role);

        return refreshToken;
    }

}
package HM.Hanbat_Market.service.account.apple.apple.utils;

import HM.Hanbat_Market.api.Result;
import HM.Hanbat_Market.domain.entity.Member;
import HM.Hanbat_Market.repository.member.MemberRepository;
import HM.Hanbat_Market.service.account.RefreshToken;
import HM.Hanbat_Market.service.account.RefreshTokenService;
import HM.Hanbat_Market.service.account.apple.apple.model.Key;
import HM.Hanbat_Market.service.account.apple.apple.model.Keys;
import HM.Hanbat_Market.service.account.apple.apple.model.Payload;
import HM.Hanbat_Market.service.account.apple.apple.model.TokenResponse;
import HM.Hanbat_Market.service.account.apple.apple.service.AppleMemberService;
import HM.Hanbat_Market.service.account.apple.apple.service.AppleService;
import HM.Hanbat_Market.service.account.apple.apple.service.AppleServiceImpl;
import HM.Hanbat_Market.service.account.jwt.CustomOAuth2User;
import HM.Hanbat_Market.service.account.jwt.JWTUtil;
import HM.Hanbat_Market.service.member.MemberService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class AppleUtils {

    @Value("${APPLE.PUBLICKEY.URL}")
    private String APPLE_PUBLIC_KEYS_URL;

    @Value("${APPLE.ISS}")
    private String ISS;

    @Value("${APPLE.AUD}")
    private String AUD;

    @Value("${APPLE.TEAM.ID}")
    private String TEAM_ID;

    @Value("${APPLE.KEY.ID}")
    private String KEY_ID;

    @Value("${APPLE.KEY.PATH}")
    private String KEY_PATH;

    @Value("${APPLE.AUTH.TOKEN.URL}")
    private String AUTH_TOKEN_URL;

    @Value("${APPLE.WEBSITE.URL}")
    private String APPLE_WEBSITE_URL;

    private final JWTUtil jwtUtil;
    private final AppleMemberService appleMemberService;
    private final RefreshTokenService refreshTokenService;
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    /**
     * User가 Sign in with Apple 요청(https://appleid.apple.com/auth/authorize)으로 전달받은 id_token을 이용한 최초 검증 Apple Document
     * URL ‣ https://developer.apple.com/documentation/sign_in_with_apple/sign_in_with_apple_rest_api/verifying_a_user
     *
     * @param id_token
     * @return boolean
     */
    public boolean verifyIdentityToken(String id_token) {

        try {
            SignedJWT signedJWT = SignedJWT.parse(id_token);
            ReadOnlyJWTClaimsSet payload = signedJWT.getJWTClaimsSet();

            // EXP
            Date currentTime = new Date(System.currentTimeMillis());
            if (!currentTime.before(payload.getExpirationTime())) {
                return false;
            }

            // NONCE(Test value), ISS, AUD
            if (!"20B20D-0S8-1K8".equals(payload.getClaim("nonce")) || !ISS.equals(payload.getIssuer()) || !AUD.equals(
                    payload.getAudience().get(0))) {
                return false;
            }

            // RSA
            if (verifyPublicKey(signedJWT)) {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Apple Server에서 공개 키를 받아서 서명 확인
     *
     * @param signedJWT
     * @return
     */
    private boolean verifyPublicKey(SignedJWT signedJWT) {

        try {
            String publicKeys = HttpClientUtils.doGet(APPLE_PUBLIC_KEYS_URL);
            ObjectMapper objectMapper = new ObjectMapper();
            Keys keys = objectMapper.readValue(publicKeys, Keys.class);
            for (Key key : keys.getKeys()) {
                RSAKey rsaKey = (RSAKey) JWK.parse(objectMapper.writeValueAsString(key));
                RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
                JWSVerifier verifier = new RSASSAVerifier(publicKey);

                if (signedJWT.verify(verifier)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * client_secret 생성 Apple Document URL ‣
     * https://developer.apple.com/documentation/sign_in_with_apple/generate_and_validate_tokens
     *
     * @return client_secret(jwt)
     */
    public String createClientSecret() throws NoSuchAlgorithmException {

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(KEY_ID).build();
        JWTClaimsSet claimsSet = new JWTClaimsSet();
        Date now = new Date();

        claimsSet.setIssuer(TEAM_ID);
        claimsSet.setIssueTime(now);
        claimsSet.setExpirationTime(new Date(now.getTime() + 3600000));
        claimsSet.setAudience(ISS);
        claimsSet.setSubject(AUD);

        SignedJWT jwt = new SignedJWT(header, claimsSet);

        byte[] privateKeyBytes = readPrivateKey();

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");

        try {
            PrivateKey privateKey = kf.generatePrivate(spec);
            ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
            BigInteger privateKeyBigInt = ecPrivateKey.getS(); // ECPrivateKey를 BigInteger로 변환

            JWSSigner jwsSigner = new ECDSASigner(privateKeyBigInt); // BigInteger로 생성
            jwt.sign(jwsSigner);

        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            // InvalidKeySpecException 처리 로직
        } catch (JOSEException e) {
            e.printStackTrace();
            // JOSEException 처리 로직
        }

        return jwt.serialize();
    }


    /**
     * 파일에서 private key 획득
     *
     * @return Private Key
     */
    private byte[] readPrivateKey() {

        Resource resource = new ClassPathResource(KEY_PATH);
        byte[] content = null;

        try (FileReader keyReader = new FileReader(resource.getURI().getPath());
             PemReader pemReader = new PemReader(keyReader)) {
            {
                PemObject pemObject = pemReader.readPemObject();
                content = pemObject.getContent();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;
    }


    /**
     * 유효한 code 인지 Apple Server에 확인 요청 Apple Document URL ‣
     * https://developer.apple.com/documentation/sign_in_with_apple/generate_and_validate_tokens
     *
     * @return
     */
    public Result validateAuthorizationGrantCode(String client_secret, String code, HttpServletResponse response)
            throws IOException, ParseException, JOSEException {

        Map<String, String> tokenRequest = new HashMap<>();

        tokenRequest.put("client_id", AUD);
        tokenRequest.put("client_secret", client_secret);
        tokenRequest.put("code", code);
        tokenRequest.put("grant_type", "authorization_code");
        tokenRequest.put("redirect_uri", APPLE_WEBSITE_URL);

        TokenResponse tokenResponse = getTokenResponse(tokenRequest);

        String email = jwtUtil.getEmail(tokenResponse.getId_token());

        Member member = appleMemberService.saveOrUpdate(email);

        String accessToken = appleMemberService.getAccessToken(member);
        String refreshToken = appleMemberService.getRefreshToken(member);

        String uuid = member.getUuid();

        Member findMember = memberRepository.findByUUID(uuid).get();

        Optional<RefreshToken> findToken = refreshTokenService.findByUuid(uuid);

        if (findToken.isPresent()) {
            memberService.updateToken(refreshToken, uuid);
        } else if (!findToken.isPresent()) {
            refreshTokenService.save(new RefreshToken(uuid, refreshToken, findMember));
        }


        response.addCookie(createCookie("Authorization", accessToken));
        response.addCookie(createCookie("AuthorizationRefresh", refreshToken));
        response.sendRedirect("https://vervet-optimal-crawdad.ngrok-free.app/clear");

        return new Result("ok");
    }

    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60 * 60 * 60);
        //cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }

    /**
     * 유효한 refresh_token 인지 Apple Server에 확인 요청 Apple Document URL ‣
     * https://developer.apple.com/documentation/sign_in_with_apple/generate_and_validate_tokens
     *
     * @param client_secret
     * @param refresh_token
     * @return
     */
    public Result validateAnExistingRefreshToken(String client_secret, String refresh_token) {

        Map<String, String> tokenRequest = new HashMap<>();

        tokenRequest.put("client_id", AUD);
        tokenRequest.put("client_secret", client_secret);
        tokenRequest.put("grant_type", "refresh_token");
        tokenRequest.put("refresh_token", refresh_token);

        return new Result("ok");
    }

    /**
     * POST https://appleid.apple.com/auth/token
     *
     * @param tokenRequest
     * @return
     */
    private TokenResponse getTokenResponse(Map<String, String> tokenRequest) {

        try {
            String response = HttpClientUtils.doPost(AUTH_TOKEN_URL, tokenRequest);
            ObjectMapper objectMapper = new ObjectMapper();
            TokenResponse tokenResponse = objectMapper.readValue(response, TokenResponse.class);

            if (tokenRequest != null) {
                return tokenResponse;
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Apple Meta Value
     *
     * @return
     */
    public Map<String, String> getMetaInfo() {

        Map<String, String> metaInfo = new HashMap<>();

        metaInfo.put("CLIENT_ID", AUD);
        metaInfo.put("REDIRECT_URI", APPLE_WEBSITE_URL);
        metaInfo.put("NONCE", "20B20D-0S8-1K8"); // Test value

        return metaInfo;
    }

    /**
     * id_token을 decode해서 payload 값 가져오기
     *
     * @param id_token
     * @return
     */
    public Payload decodeFromIdToken(String id_token) {

        try {
            SignedJWT signedJWT = SignedJWT.parse(id_token);
            log.info(id_token, "@@@@@@@@아이디 토큰");
            ReadOnlyJWTClaimsSet getPayload = signedJWT.getJWTClaimsSet();
            ObjectMapper objectMapper = new ObjectMapper();
            Payload payload = objectMapper.readValue(getPayload.toJSONObject().toJSONString(), Payload.class);

            if (payload != null) {
                return payload;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
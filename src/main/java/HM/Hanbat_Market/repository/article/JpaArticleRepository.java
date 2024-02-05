package HM.Hanbat_Market.repository.article;

import HM.Hanbat_Market.domain.entity.Article;
import HM.Hanbat_Market.domain.entity.Item;
import HM.Hanbat_Market.domain.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaArticleRepository implements ArticleRepository {

    private final EntityManager em;

    @Override
    public Article save(Article article) {
        em.persist(article);
        return article;
    }

    @Override
    public Optional<Article> findById(Long id) {
        Article article = em.find(Article.class, id);
        return Optional.ofNullable(article);
    }

    @Override
    public List<Article> findAll() {
        List<Article> articles = em.createQuery("select a from Article a", Article.class)
                .getResultList();
        return articles;
    }

    @Override
    public List<Article> findAllByMember(Member member) {
        Long memberId = member.getId();
        return em.createQuery("select a from Article a join a.member m where m.id = :memberId")
                .setParameter("memberId", member.getId())
                .getResultList();
    }

    @Override
    public List<Article> findAllBySearch(ArticleSearchDto articleSearchDto) {
        //language=JPAQL
        String jpql = "select a From Article a join a.item i";
        boolean isFirstCondition = true;
        //아이템 상태 검색
        if (articleSearchDto.getItemStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " i.itemStatus = :itemStatus";
        }
        //아이템 이름 검색
        if (StringUtils.hasText(articleSearchDto.getItemName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " i.itemName like :itemName";
        }
        TypedQuery<Article> query = em.createQuery(jpql, Article.class)
                .setMaxResults(1000); //최대 1000건
        if (articleSearchDto.getItemStatus() != null) {
            query = query
                    .setParameter("itemStatus", articleSearchDto.getItemStatus());
        }
        if (StringUtils.hasText(articleSearchDto.getItemName())) {
            query = query
                    .setParameter("itemName", articleSearchDto.getItemName());
        }
        return query.getResultList();
    }
}
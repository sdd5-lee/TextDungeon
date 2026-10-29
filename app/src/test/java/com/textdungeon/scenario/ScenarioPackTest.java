package com.textdungeon.scenario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.textdungeon.billing.Entitlements;
import com.textdungeon.billing.Product;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// ══════════════════════════════════════════════
//  ScenarioPack / ScenarioRepository 테스트
// ══════════════════════════════════════════════
public class ScenarioPackTest {

    private final ScenarioRepository repo = new ScenarioRepository();

    @Test
    public void 없거나_모르는_ID는_기본_시나리오() {
        assertEquals(ScenarioPack.BASE, ScenarioPack.fromId(null));
        assertEquals(ScenarioPack.BASE, ScenarioPack.fromId(""));
        assertEquals(ScenarioPack.BASE, ScenarioPack.fromId("deleted_dlc"));
    }

    @Test
    public void fromId로_모든_시나리오가_되돌아온다() {
        for (ScenarioPack p : ScenarioPack.values()) {
            assertEquals(p, ScenarioPack.fromId(p.id));
        }
    }

    @Test
    public void 유료_시나리오는_SCENARIO_상품에_연결된다() {
        Set<Product> used = new HashSet<>();
        for (ScenarioPack p : ScenarioPack.values()) {
            if (p.product == null) continue;
            assertEquals(Product.Kind.SCENARIO, p.product.kind);
            assertTrue("상품을 두 시나리오가 같이 씀: " + p.product, used.add(p.product));
            assertTrue("DLC는 assets 폴더가 있어야 함: " + p, !p.assetDir.isEmpty());
        }
    }

    @Test
    public void 기본_시나리오는_보유_없어도_열려_있다() {
        assertTrue(repo.isUnlocked(ScenarioPack.BASE, new Entitlements(Collections.<String>emptySet())));
    }

    @Test
    public void 기본_시나리오_경로는_지금_쓰는_파일_그대로() {
        assertEquals("event_list.json", repo.assetPath(ScenarioPack.BASE, ScenarioRepository.EVENTS));
        assertEquals("monster_list.json", repo.assetPath(ScenarioPack.BASE, ScenarioRepository.MONSTERS));
        assertEquals("item_list.json", repo.assetPath(ScenarioPack.BASE, ScenarioRepository.ITEMS));
    }
}

package com.textdungeon.scenario;

import com.textdungeon.billing.Product;

// ══════════════════════════════════════════════
//  시나리오 = 이벤트·몬스터·아이템 JSON 묶음. 직업·특성·마법은 코드와 공통 파일이라 모든 시나리오에서 같다
//  DLC를 더할 때:
//   1) assets/scenarios/<id>/ 에 event_list.json, monster_list.json, item_list.json을 넣는다
//      (도감이 섞이지 않게 ID에 시나리오 접두사를 붙인다. 예: frost_slime)
//   2) Product에 SCENARIO 상품을 한 줄 더한다 (예: SCENARIO_FROST("scenario_frost", ...))
//   3) 여기에 한 줄 더한다 (예: FROST("frost", "얼어붙은 성채", Product.SCENARIO_FROST, "scenarios/frost"))
// ══════════════════════════════════════════════
public enum ScenarioPack {
    /** 기본 던전. 무료이고 지금 쓰는 assets 경로(폴더 없음)를 그대로 쓴다 */
    BASE("base", "기본 던전", null, "");

    public final String id;
    public final String displayName;
    /** null이면 무료 */
    public final Product product;
    /** assets 안의 폴더. 빈 문자열이면 assets 바로 아래 */
    public final String assetDir;

    ScenarioPack(String id, String displayName, Product product, String assetDir) {
        this.id = id;
        this.displayName = displayName;
        this.product = product;
        this.assetDir = assetDir;
    }

    /** null이거나 모르는 ID면 BASE (시나리오가 없던 예전 세이브 호환) */
    public static ScenarioPack fromId(String id) {
        if (id == null) return BASE;
        for (ScenarioPack p : values()) {
            if (p.id.equals(id)) return p;
        }
        return BASE;
    }
}

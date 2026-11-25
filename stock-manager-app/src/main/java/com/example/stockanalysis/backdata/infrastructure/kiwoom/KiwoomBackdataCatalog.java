package com.example.stockanalysis.backdata.infrastructure.kiwoom;

import com.example.stockanalysis.backdata.validation.model.BackdataDatasetType;
import com.example.stockanalysis.backdata.validation.model.BackdataSchema;
import com.example.stockanalysis.backdata.validation.model.MapBackdataSchemaRegistry;
import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KiwoomBackdataCatalog {

    private static final Logger log = LoggerFactory.getLogger(KiwoomBackdataCatalog.class);
    private static final String CONTRACT_PACKAGE =
            "com.example.stockanalysis.kiwoom.contract.tr.";

    private final Map<String, KiwoomBackdataTrDefinition> definitions;
    private final MapBackdataSchemaRegistry schemaRegistry;

    public KiwoomBackdataCatalog() {
        Map<String, KiwoomBackdataTrDefinition> items = new LinkedHashMap<>();
        register(items, definition(
                "ka10099", "종목정보 리스트", "/api/dostk/stkinfo",
                BackdataDatasetType.STOCK_MASTER, "list",
                "code", null, null, null, null,
                fields(
                        "code", "stockCode",
                        "name", "name",
                        "listCount", "listedShares",
                        "auditInfo", "auditInfo",
                        "regDay", "listedDate",
                        "lastPrice", "lastPrice",
                        "state", "state",
                        "marketCode", "marketCode",
                        "marketName", "marketName",
                        "upName", "industryName",
                        "upSizeName", "companySize",
                        "companyClassName", "companyClass",
                        "orderWarning", "orderWarning",
                        "nxtEnable", "nxtEnabled"
                ),
                true, false, true, false,
                set("name"), set("listedShares", "lastPrice"),
                set("listedShares", "lastPrice")
        ));
        register(items, definition(
                "ka10100", "종목정보 조회", "/api/dostk/stkinfo",
                BackdataDatasetType.STOCK_DETAIL, null,
                "code", null, null, "stk_cd", null,
                fields(
                        "code", "stockCode",
                        "name", "name",
                        "listCount", "listedShares",
                        "auditInfo", "auditInfo",
                        "regDay", "listedDate",
                        "lastPrice", "lastPrice",
                        "state", "state",
                        "marketCode", "marketCode",
                        "marketName", "marketName",
                        "upName", "industryName",
                        "upSizeName", "companySize",
                        "companyClassName", "companyClass",
                        "orderWarning", "orderWarning",
                        "nxtEnable", "nxtEnabled"
                ),
                true, false, true, false,
                set("name"), set("listedShares", "lastPrice"),
                set("listedShares", "lastPrice")
        ));
        register(items, definition(
                "ka10001", "주식기본정보요청", "/api/dostk/stkinfo",
                BackdataDatasetType.STOCK_FUNDAMENTAL_SNAPSHOT, null,
                "stk_cd", null, null, "stk_cd", null,
                fields(
                        "stk_cd", "stockCode",
                        "stk_nm", "name",
                        "flo_stk", "listedShares",
                        "mac", "marketCap",
                        "per", "per",
                        "eps", "eps",
                        "roe", "roe",
                        "pbr", "pbr",
                        "bps", "bps",
                        "sale_amt", "revenue",
                        "bus_pro", "operatingProfit",
                        "cur_prc", "currentPrice"
                ),
                true, false, true, false,
                set("name"),
                set("listedShares", "marketCap", "per", "eps", "roe", "pbr",
                        "bps", "revenue", "operatingProfit", "currentPrice"),
                set("listedShares", "marketCap", "currentPrice")
        ));
        register(items, definition(
                "ka10101", "업종코드 리스트", "/api/dostk/stkinfo",
                BackdataDatasetType.INDUSTRY_MASTER, "list",
                null, "code", null, null, null,
                fields(
                        "code", "industryCode",
                        "name", "name",
                        "group", "group"
                ),
                false, true, true, false,
                set("industryCode", "name"), set(), set()
        ));
        register(items, definition(
                "ka10080", "주식분봉차트조회요청", "/api/dostk/chart",
                BackdataDatasetType.MINUTE_CANDLE, "stk_min_pole_chart_qry",
                null, null, "cntr_tm", "stk_cd", null,
                fields(
                        "cur_prc", "close",
                        "trde_qty", "volume",
                        "cntr_tm", "timestamp",
                        "open_pric", "open",
                        "high_pric", "high",
                        "low_pric", "low",
                        "pred_pre", "change",
                        "acc_trde_qty", "accumulatedVolume"
                ),
                true, false, true, true,
                set("open", "high", "low", "close", "volume", "timestamp"),
                set("open", "high", "low", "close", "volume", "change",
                        "accumulatedVolume"),
                set("open", "high", "low", "close", "volume", "accumulatedVolume")
        ));
        register(items, definition(
                "ka10081", "주식일봉차트조회요청", "/api/dostk/chart",
                BackdataDatasetType.DAILY_CANDLE, "stk_dt_pole_chart_qry",
                null, null, "dt", "stk_cd", null,
                fields(
                        "cur_prc", "close",
                        "trde_qty", "volume",
                        "trde_prica", "tradingAmount",
                        "dt", "timestamp",
                        "open_pric", "open",
                        "high_pric", "high",
                        "low_pric", "low",
                        "pred_pre", "change",
                        "trde_tern_rt", "turnoverRate"
                ),
                true, false, true, true,
                set("open", "high", "low", "close", "volume"),
                set("open", "high", "low", "close", "volume", "tradingAmount",
                        "change", "turnoverRate"),
                set("open", "high", "low", "close", "volume", "tradingAmount")
        ));
        register(items, definition(
                "ka10082", "주식주봉차트조회요청", "/api/dostk/chart",
                BackdataDatasetType.WEEKLY_CANDLE, "stk_stk_pole_chart_qry",
                null, null, "dt", "stk_cd", null,
                fields(
                        "cur_prc", "close",
                        "trde_qty", "volume",
                        "trde_prica", "tradingAmount",
                        "dt", "timestamp",
                        "open_pric", "open",
                        "high_pric", "high",
                        "low_pric", "low",
                        "pred_pre", "change",
                        "trde_tern_rt", "turnoverRate"
                ),
                true, false, true, true,
                set("open", "high", "low", "close", "volume", "timestamp"),
                set("open", "high", "low", "close", "volume", "tradingAmount",
                        "change", "turnoverRate"),
                set("open", "high", "low", "close", "volume", "tradingAmount")
        ));
        register(items, definition(
                "ka10083", "주식월봉차트조회요청", "/api/dostk/chart",
                BackdataDatasetType.MONTHLY_CANDLE, "stk_mth_pole_chart_qry",
                null, null, "dt", "stk_cd", null,
                fields(
                        "cur_prc", "close",
                        "trde_qty", "volume",
                        "trde_prica", "tradingAmount",
                        "dt", "timestamp",
                        "open_pric", "open",
                        "high_pric", "high",
                        "low_pric", "low",
                        "pred_pre", "change",
                        "trde_tern_rt", "turnoverRate"
                ),
                true, false, true, true,
                set("open", "high", "low", "close", "volume", "timestamp"),
                set("open", "high", "low", "close", "volume", "tradingAmount",
                        "change", "turnoverRate"),
                set("open", "high", "low", "close", "volume", "tradingAmount")
        ));
        register(items, definition(
                "ka10086", "일별주가요청", "/api/dostk/mrkcond",
                BackdataDatasetType.DAILY_PRICE, "daly_stkpc",
                null, null, "date", "stk_cd", null,
                fields(
                        "open_pric", "open",
                        "high_pric", "high",
                        "low_pric", "low",
                        "close_pric", "close",
                        "trde_qty", "volume",
                        "amt_mn", "tradingAmount",
                        "crd_rt", "creditRatio",
                        "ind", "individual",
                        "orgn", "institution",
                        "frgn", "foreigner",
                        "prm", "program",
                        "for_poss", "foreignHolding",
                        "for_wght", "foreignHoldingRatio",
                        "for_netprps", "foreignNetBuy",
                        "orgn_netprps", "institutionNetBuy",
                        "ind_netprps", "individualNetBuy",
                        "crd_remn_rt", "creditBalanceRatio"
                ),
                true, false, true, true,
                set("open", "high", "low", "close", "volume"),
                set("open", "high", "low", "close", "volume", "tradingAmount",
                        "creditRatio", "individual", "institution", "foreigner",
                        "program", "foreignHolding", "foreignHoldingRatio",
                        "foreignNetBuy", "institutionNetBuy", "individualNetBuy",
                        "creditBalanceRatio"),
                set("open", "high", "low", "close", "volume", "tradingAmount",
                        "foreignHolding", "foreignHoldingRatio")
        ));
        register(items, definition(
                "ka10015", "일별거래상세요청", "/api/dostk/stkinfo",
                BackdataDatasetType.DAILY_TRADE_DETAIL, "daly_trde_dtl",
                null, null, "dt", "stk_cd", null,
                fields(
                        "close_pric", "close",
                        "trde_qty", "volume",
                        "trde_prica", "tradingAmount",
                        "bf_mkrt_trde_qty", "preMarketVolume",
                        "opmr_trde_qty", "regularMarketVolume",
                        "af_mkrt_trde_qty", "afterMarketVolume",
                        "cntr_str", "executionStrength",
                        "for_netprps", "foreignNetBuy",
                        "orgn_netprps", "institutionNetBuy",
                        "ind_netprps", "individualNetBuy",
                        "crd_remn_rt", "creditBalanceRatio",
                        "prm", "program"
                ),
                true, false, true, true,
                set("close", "volume"),
                set("close", "volume", "tradingAmount", "preMarketVolume",
                        "regularMarketVolume", "afterMarketVolume", "executionStrength",
                        "foreignNetBuy", "institutionNetBuy", "individualNetBuy",
                        "creditBalanceRatio", "program"),
                set("close", "volume", "tradingAmount", "preMarketVolume",
                        "regularMarketVolume", "afterMarketVolume")
        ));
        register(items, definition(
                "ka20006", "업종일봉조회요청", "/api/dostk/chart",
                BackdataDatasetType.INDUSTRY_DAILY_CANDLE, "inds_dt_pole_qry",
                null, null, "dt", null, "inds_cd",
                fields(
                        "cur_prc", "close",
                        "trde_qty", "volume",
                        "trde_prica", "tradingAmount",
                        "open_pric", "open",
                        "high_pric", "high",
                        "low_pric", "low"
                ),
                false, true, true, true,
                set("open", "high", "low", "close", "volume"),
                set("open", "high", "low", "close", "volume", "tradingAmount"),
                set("open", "high", "low", "close", "volume", "tradingAmount")
        ));
        register(items, definition(
                "ka10059", "종목별투자자기관별요청", "/api/dostk/stkinfo",
                BackdataDatasetType.INVESTOR_TRADING, "stk_invsr_orgn",
                null, null, "dt", "stk_cd", null,
                fields(
                        "cur_prc", "close",
                        "acc_trde_qty", "volume",
                        "acc_trde_prica", "tradingAmount",
                        "ind_invsr", "individual",
                        "frgnr_invsr", "foreigner",
                        "orgn", "institution",
                        "fnnc_invt", "financialInvestment",
                        "insrnc", "insurance",
                        "invtrt", "investmentTrust",
                        "etc_fnnc", "otherFinance",
                        "bank", "bank",
                        "penfnd_etc", "pension",
                        "samo_fund", "privateFund",
                        "natn", "government",
                        "etc_corp", "otherCorporation",
                        "natfor", "otherForeign"
                ),
                true, false, true, true,
                set("individual", "foreigner", "institution"),
                set("close", "volume", "tradingAmount", "individual", "foreigner",
                        "institution", "financialInvestment", "insurance",
                        "investmentTrust", "otherFinance", "bank", "pension",
                        "privateFund", "government", "otherCorporation", "otherForeign"),
                set("close", "volume", "tradingAmount")
        ));
        registerExtended(items);
        definitions = Map.copyOf(items);
        schemaRegistry = new MapBackdataSchemaRegistry(
                items.values().stream().map(KiwoomBackdataTrDefinition::schema).toList()
        );
        long datasetTypeCount = items.values().stream()
                .map(definition -> definition.schema().datasetType())
                .distinct()
                .count();
        log.info(
                "백데이터 전문 카탈로그 초기화 완료. trCount={}, datasetTypeCount={}",
                definitions.size(),
                datasetTypeCount
        );
    }

    public KiwoomBackdataTrDefinition getRequired(String apiId) {
        KiwoomBackdataTrDefinition definition = definitions.get(apiId);
        if (definition == null) {
            throw new IllegalArgumentException("지원하지 않는 백데이터 전문입니다: " + apiId);
        }
        return definition;
    }

    public Collection<KiwoomBackdataTrDefinition> all() {
        return List.copyOf(definitions.values());
    }

    public MapBackdataSchemaRegistry schemaRegistry() {
        return schemaRegistry;
    }

    private void registerExtended(Map<String, KiwoomBackdataTrDefinition> items) {
        register(items, definition(
                "ka10008", "주식외국인종목별매매동향", "/api/dostk/frgnistt",
                BackdataDatasetType.FOREIGN_HOLDING, "stk_frgnr",
                null, null, "dt", "stk_cd", null,
                fields(
                        "close_pric", "close", "trde_qty", "volume",
                        "chg_qty", "holdingChange", "poss_stkcnt", "holdingQuantity",
                        "wght", "holdingRatio", "gain_pos_stkcnt", "acquiredQuantity",
                        "frgnr_limit", "foreignLimit", "frgnr_limit_irds", "foreignLimitChange",
                        "limit_exh_rt", "foreignLimitExhaustionRatio"
                ),
                true, false, true, true,
                set("close", "holdingQuantity"),
                set("close", "volume", "holdingChange", "holdingQuantity",
                        "holdingRatio", "acquiredQuantity", "foreignLimit",
                        "foreignLimitChange", "foreignLimitExhaustionRatio"),
                set("close", "volume", "holdingQuantity", "holdingRatio",
                        "acquiredQuantity", "foreignLimit", "foreignLimitExhaustionRatio")
        ));
        register(items, definition(
                "ka10013", "신용매매동향요청", "/api/dostk/stkinfo",
                BackdataDatasetType.CREDIT_TREND, "crd_trde_trend",
                null, null, "dt", "stk_cd", null,
                fields(
                        "cur_prc", "close", "trde_qty", "volume",
                        "new", "newCredit", "rpya", "repayment",
                        "remn", "creditBalance", "amt", "creditAmount",
                        "shr_rt", "creditShareRatio", "remn_rt", "creditBalanceRatio"
                ),
                true, false, true, true,
                set("creditBalance"),
                set("close", "volume", "newCredit", "repayment", "creditBalance",
                        "creditAmount", "creditShareRatio", "creditBalanceRatio"),
                set("close", "volume", "newCredit", "repayment", "creditBalance",
                        "creditAmount", "creditShareRatio", "creditBalanceRatio")
        ));
        register(items, definition(
                "ka10014", "공매도추이요청", "/api/dostk/shsa",
                BackdataDatasetType.SHORT_SELLING, "shrts_trnsn",
                null, null, "dt", "stk_cd", null,
                fields(
                        "close_pric", "close", "trde_qty", "volume",
                        "shrts_qty", "shortVolume", "ovr_shrts_qty", "accumulatedShortVolume",
                        "trde_wght", "shortRatio", "shrts_trde_prica", "shortTradingAmount",
                        "shrts_avg_pric", "shortAveragePrice"
                ),
                true, false, true, true,
                set("volume", "shortVolume"),
                set("close", "volume", "shortVolume", "accumulatedShortVolume",
                        "shortRatio", "shortTradingAmount", "shortAveragePrice"),
                set("close", "volume", "shortVolume", "accumulatedShortVolume",
                        "shortRatio", "shortTradingAmount", "shortAveragePrice")
        ));
        register(items, definition(
                "ka20068", "대차거래추이요청(종목별)", "/api/dostk/slb",
                BackdataDatasetType.STOCK_LENDING, "dbrt_trde_trnsn",
                null, null, "dt", "stk_cd", null,
                fields(
                        "dbrt_trde_cntrcnt", "loanContracts",
                        "dbrt_trde_rpy", "loanRepayments",
                        "dbrt_trde_irds", "loanChange",
                        "rmnd", "loanBalance",
                        "remn_amt", "loanBalanceAmount"
                ),
                true, false, true, true,
                set("loanBalance"),
                set("loanContracts", "loanRepayments", "loanChange",
                        "loanBalance", "loanBalanceAmount"),
                set("loanContracts", "loanRepayments", "loanBalance", "loanBalanceAmount")
        ));
        register(items, definition(
                "ka90013", "종목일별프로그램매매추이요청", "/api/dostk/mrkcond",
                BackdataDatasetType.PROGRAM_TRADING, "stk_daly_prm_trde_trnsn",
                null, null, "dt", "stk_cd", null,
                fields(
                        "cur_prc", "close", "trde_qty", "volume",
                        "prm_sell_amt", "programSellAmount",
                        "prm_buy_amt", "programBuyAmount",
                        "prm_netprps_amt", "programNetBuyAmount",
                        "prm_sell_qty", "programSellQuantity",
                        "prm_buy_qty", "programBuyQuantity",
                        "prm_netprps_qty", "programNetBuyQuantity",
                        "dbrt_trde_rpy_sum", "loanRepaymentSum",
                        "remn_rcvord_sum", "loanBalanceReceiptSum",
                        "stex_tp", "exchangeType"
                ),
                true, false, true, true,
                set("programSellAmount", "programBuyAmount"),
                set("close", "volume", "programSellAmount", "programBuyAmount",
                        "programNetBuyAmount", "programSellQuantity", "programBuyQuantity",
                        "programNetBuyQuantity", "loanRepaymentSum", "loanBalanceReceiptSum"),
                set("close", "volume", "programSellAmount", "programBuyAmount",
                        "programSellQuantity", "programBuyQuantity",
                        "loanRepaymentSum", "loanBalanceReceiptSum")
        ));
        register(items, definition(
                "ka10045", "종목별기관매매추이요청", "/api/dostk/mrkcond",
                BackdataDatasetType.INSTITUTION_TREND, "stk_orgn_trde_trnsn",
                null, null, "dt", "stk_cd", null,
                fields(
                        "close_pric", "close", "trde_qty", "volume",
                        "orgn_dt_acc", "institutionAccumulated",
                        "orgn_daly_nettrde_qty", "institutionDailyNetBuy",
                        "for_dt_acc", "foreignAccumulated",
                        "for_daly_nettrde_qty", "foreignDailyNetBuy",
                        "limit_exh_rt", "foreignLimitExhaustionRatio"
                ),
                true, false, true, true,
                set("institutionDailyNetBuy", "foreignDailyNetBuy"),
                set("close", "volume", "institutionAccumulated",
                        "institutionDailyNetBuy", "foreignAccumulated",
                        "foreignDailyNetBuy", "foreignLimitExhaustionRatio"),
                set("close", "volume", "foreignLimitExhaustionRatio")
        ));
        register(items, definition(
                "ka10047", "체결강도추이일별요청", "/api/dostk/mrkcond",
                BackdataDatasetType.EXECUTION_STRENGTH, "cntr_str_daly",
                null, null, "dt", "stk_cd", null,
                fields(
                        "cur_prc", "close", "trde_qty", "volume",
                        "acc_trde_prica", "tradingAmount",
                        "cntr_str", "executionStrength",
                        "cntr_str_5min", "executionStrength5m",
                        "cntr_str_20min", "executionStrength20m",
                        "cntr_str_60min", "executionStrength60m"
                ),
                true, false, true, true,
                set("executionStrength"),
                set("close", "volume", "tradingAmount", "executionStrength",
                        "executionStrength5m", "executionStrength20m", "executionStrength60m"),
                set("close", "volume", "tradingAmount", "executionStrength",
                        "executionStrength5m", "executionStrength20m", "executionStrength60m")
        ));
        register(items, definition(
                "ka90001", "테마그룹별요청", "/api/dostk/thme",
                BackdataDatasetType.THEME_GROUP, "thema_grp",
                null, "thema_grp_cd", null, null, null,
                fields(
                        "thema_grp_cd", "themeCode", "thema_nm", "name",
                        "stk_num", "stockCount", "flu_rt", "changeRate",
                        "rising_stk_num", "risingStockCount",
                        "fall_stk_num", "fallingStockCount",
                        "dt_prft_rt", "periodReturn", "main_stk", "mainStock"
                ),
                false, true, true, false,
                set("themeCode", "name"),
                set("stockCount", "changeRate", "risingStockCount",
                        "fallingStockCount", "periodReturn"),
                set("stockCount", "risingStockCount", "fallingStockCount")
        ));
        register(items, definition(
                "ka90002", "테마구성종목요청", "/api/dostk/thme",
                BackdataDatasetType.THEME_CONSTITUENT, "thema_comp_stk",
                "stk_cd", null, null, null, "thema_grp_cd",
                fields(
                        "stk_cd", "stockCode", "stk_nm", "name",
                        "cur_prc", "close", "flu_rt", "changeRate",
                        "acc_trde_qty", "volume", "dt_prft_rt_n", "periodReturn"
                ),
                true, true, true, false,
                set("name"),
                set("close", "changeRate", "volume", "periodReturn"),
                set("close", "volume")
        ));
    }

    private KiwoomBackdataTrDefinition definition(
            String apiId,
            String name,
            String uri,
            BackdataDatasetType datasetType,
            String rowField,
            String rowStockCodeField,
            String rowReferenceCodeField,
            String businessDateField,
            String requestStockCodeField,
            String requestReferenceCodeField,
            Map<String, String> mappings,
            boolean stockCodeRequired,
            boolean referenceCodeRequired,
            boolean businessDateRequired,
            boolean historical,
            Set<String> requiredFields,
            Set<String> numericFields,
            Set<String> nonNegativeFields
    ) {
        BackdataSchema schema = new BackdataSchema(
                apiId,
                datasetType,
                stockCodeRequired,
                referenceCodeRequired,
                businessDateRequired,
                historical,
                requiredFields,
                numericFields,
                nonNegativeFields
        );
        return new KiwoomBackdataTrDefinition(
                apiId,
                name,
                uri,
                loadRequestType(apiId),
                loadResponseType(apiId),
                rowField,
                rowStockCodeField,
                rowReferenceCodeField,
                businessDateField,
                requestStockCodeField,
                requestReferenceCodeField,
                variantFields(apiId),
                mappings,
                schema
        );
    }

    private void register(
            Map<String, KiwoomBackdataTrDefinition> definitions,
            KiwoomBackdataTrDefinition definition
    ) {
        if (definitions.put(definition.apiId(), definition) != null) {
            throw new IllegalStateException("중복된 키움 전문 정의: " + definition.apiId());
        }
    }

    private Class<?> loadRequestType(String apiId) {
        return loadType(apiId, "Request");
    }

    @SuppressWarnings("unchecked")
    private Class<? extends KiwoomResult> loadResponseType(String apiId) {
        return (Class<? extends KiwoomResult>) loadType(apiId, "Response");
    }

    private Class<?> loadType(String apiId, String suffix) {
        String simpleName = Character.toUpperCase(apiId.charAt(0))
                + apiId.substring(1)
                + suffix;
        try {
            return Class.forName(CONTRACT_PACKAGE + apiId + "." + simpleName);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("키움 전문 DTO를 찾을 수 없습니다: " + apiId, exception);
        }
    }

    private static Set<String> set(String... values) {
        return Set.of(values);
    }

    private static Set<String> variantFields(String apiId) {
        return switch (apiId) {
            case "ka10080" -> set("tic_scope", "upd_stkpc_tp");
            case "ka10081", "ka10082", "ka10083" -> set("upd_stkpc_tp");
            case "ka10086" -> set("indc_tp");
            case "ka10059" -> set("amt_qty_tp", "trde_tp", "unit_tp");
            case "ka10013" -> set("qry_tp");
            case "ka10014" -> set("tm_tp");
            case "ka20068" -> set("all_tp");
            case "ka90013" -> set("amt_qty_tp");
            case "ka10045" -> set("orgn_prsm_unp_tp", "for_prsm_unp_tp");
            case "ka90001" -> set("qry_tp", "date_tp", "flu_pl_amt_tp", "stex_tp");
            case "ka90002" -> set("date_tp", "stex_tp");
            default -> set();
        };
    }

    private static Map<String, String> fields(String... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("필드 매핑은 원본/표준 이름 쌍이어야 합니다.");
        }
        Map<String, String> mappings = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            mappings.put(values[index], values[index + 1]);
        }
        return mappings;
    }
}

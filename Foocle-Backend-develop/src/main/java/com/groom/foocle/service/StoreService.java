package com.groom.foocle.service;

import com.groom.foocle.dto.req.StoreDtoReq;
import com.groom.foocle.dto.res.MyStoreSimple;
import com.groom.foocle.dto.res.StoreDtoRes;

import java.util.List;

public interface StoreService {
    StoreDtoRes.IdRes create(Long userId, StoreDtoReq.Create req);
    List<MyStoreSimple> getMyStoresSimple(Long userId);
    StoreDtoRes.Detail getDetail(Long userId, Long storeId);
}

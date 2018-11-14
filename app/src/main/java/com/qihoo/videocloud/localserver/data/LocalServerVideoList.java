
package com.qihoo.videocloud.localserver.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qihoo.videocloud.localserver.VideoItemData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liuyanqing on 2017/6/30.
 */

public class LocalServerVideoList {

    private static String VIDEO_RES = "[\n" +
            "    {\n" +
            "        \"image\":\"http://p2.qhimg.com/d/inn/59127791/img_0.png\",\n" +
            "        \"url\":\"http://q3.v.k.360kan.com/vod-xinxiliu-tv-q3-bj/15726_632071bae2f98-5190-4a82-be2a-23772d9583b0.mp4\",\n" +
            "        \"rid\":\"3320c69518a97d5a6d1dbf00e6d22a24\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p2.qhimg.com/d/inn/59127791/img_1.png\",\n" +
            "        \"url\":\"http://video.mp.sj.360.cn/vod_zhushou/vod-shouzhu-bj/93f5f7529bf85bb0ed7b156f7a24eaed.mp4\",\n" +
            "        \"rid\":\"e7bb72ba9e84c0b35bb3411cddff5093\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p6.qhimg.com/d/inn/59127791/img_2.png\",\n" +
            "        \"url\":\"http://video.mp.sj.360.cn/vod_zhushou/vod-shouzhu-bj/1f212d18f71c15a07414de5ae49acb22.mp4\",\n" +
            "        \"rid\":\"ecb3cf59e32aa08b9f2be02682dedc48\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p0.qhimg.com/d/inn/59127791/img_3.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNy8zNzY3MmYzYjhjMzYyNGVmZTdlYzdiZDMxMTQ0ZDkzMS5tcDQ%3D\",\n" +
            "        \"rid\":\"5f2ef312010129af58f2a13debe4bd13\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p0.qhimg.com/d/inn/59127791/img_4.png\",\n" +
            "        \"url\":\"http://video.mp.sj.360.cn/vod_zhushou/vod-shouzhu-bj/b9d245e8e09cc0dd56f9b60152d09793.mp4\",\n" +
            "        \"rid\":\"73759959cfb978aa37332d942ac76571\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p1.qhimg.com/d/inn/17bc0a81/img_5.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNy9jYjc2NGE0Y2ViMjc2YjM0ZDc4YTFiY2ExYmY1ZWMxMy5tcDQ%3D\",\n" +
            "        \"rid\":\"e4401dc28d0a7a2b769c3c4f2440e98d\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p7.qhimg.com/d/inn/17bc0a81/img_6.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNy8zZmViYWU4NzkzYzAzYzQyZjEzNjg2OTBhZjhiNmE2OC5tcDQ%3D\",\n" +
            "        \"rid\":\"217586bb615913d96bf367249dbd9571\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p1.qhimg.com/d/inn/17bc0a81/img_7.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi9lMzEwMDNjOTUxZjJhOWNlNDNkNmUzZjg5MjVkNDcyZS5tcDQ%3D\",\n" +
            "        \"rid\":\"b2720d264bc0676e0c737a1951572e6e\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p2.qhimg.com/d/inn/17bc0a81/img_8.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNy84NTRkOGVmN2RjM2FlMGMzMTA0YTZhZmMwODAzZTZjMi5tcDQ%3D\",\n" +
            "        \"rid\":\"f49e3b5938f40a32e8a0f9205c434774\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p2.qhimg.com/d/inn/17bc0a81/img_9.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi8yZjI3MWMzNDAxNWE1MThiNDdkNzBiN2U0MmY4ZWNkNC5tcDQ%3D\",\n" +
            "        \"rid\":\"74883df9257d1706d786fb43a0e7e4c6\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p0.qhimg.com/d/inn/17bc0a81/img_10.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi9kODAyMmVmZTNlNGVhMjFlMjE2NDZhMWVlMjNiMmIwZi5tcDQ%3D\",\n" +
            "        \"rid\":\"0a6c2e43518bda97308cd3819a207c2e\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p5.qhimg.com/d/inn/cea3d896/img_11.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi84YjAwYmZjZDA4YTFhN2VkN2VmZmRiYWQ5M2YyMWY3YS5tcDQ%3D\",\n" +
            "        \"rid\":\"0dbf975259efb550fc6eef45127cb543\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p1.qhimg.com/d/inn/cea3d896/img_12.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi83OWM5ZTk4ZGUxZjM2OTMwZGJmMDgxZDNjNDk5ODNkYS5tcDQ%3D\",\n" +
            "        \"rid\":\"31f709f1e121121b827e3a22858d2496\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p3.qhimg.com/d/inn/cea3d896/img_13.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi9iMWFhMzNiYmZiNDYyNDJlN2QyZjMyZGFkMTE0ZjViYi5tcDQ%3D\",\n" +
            "        \"rid\":\"096d0a33e39824cd1976af1e760fc153\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p1.qhimg.com/d/inn/cea3d896/img_14.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi9iMDIyNzhkM2M0ZGU1NzBmNjZjZGYxMDViZGNlYmFmMC5tcDQ%3D\",\n" +
            "        \"rid\":\"f22f36ed8cc6be418c2c21d72a500c86\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p7.qhimg.com/d/inn/cea3d896/img_15.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi9kYjc3MzZlYmExMTJkODI4OWQ4OWUzYjhjZmY0MGQ2My5tcDQ%3D\",\n" +
            "        \"rid\":\"5ad7971ed80d441954b68b8266863994\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p2.qhimg.com/d/inn/b2a8f96f/img_16.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi9iMGJlMmNmNThiZWEyYTIzODc0NDQxNWJkMGE2NzEwNS5tcDQ%3D\",\n" +
            "        \"rid\":\"660d6c1dd062507c779869f1ce0470cb\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p2.qhimg.com/d/inn/b2a8f96f/img_17.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi9lYWI5ZTcxOGI2ZjZmMzQyMTA1M2YwZDg4MDZkZmE5Ni5tcDQ%3D\",\n" +
            "        \"rid\":\"4872d5b3f7dffcc33bbc93b614cf2794\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p9.qhimg.com/d/inn/b2a8f96f/img_18.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi84ZWZlYzNlZjE3ZWFkNzBhYjZkOWJlZmMzY2Q4YmE2Ni5tcDQ%3D\",\n" +
            "        \"rid\":\"097dfa6a2d12b0aec8d4c8ac27a8d31f\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p5.qhimg.com/d/inn/b2a8f96f/img_19.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi80MDE4MjA1ODIyZjU0ZjRjYjBiNzIzNTk3YTM1ZWMxNC5tcDQ%3D\",\n" +
            "        \"rid\":\"bcce28ecbb0fd862db6cd57facbab0d3\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p0.qhimg.com/d/inn/b2a8f96f/img_20.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi8zMjhjZDliM2Y3OGU0NGIyYThkNjJmMGMyMjQ3OTc0Yy5tcDQ%3D\",\n" +
            "        \"rid\":\"51b14399d5e8d30a67da07f518c83f29\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p6.qhimg.com/d/inn/b2a8f96f/img_21.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi9lNGEyYjdjOGU0ZGYxZTIzNDdjYjVjMmMxNWRkMjU2NS5tcDQ%3D\",\n" +
            "        \"rid\":\"ac6ae5acd18d7161bc480eb725d4792a\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p0.qhimg.com/d/inn/b2a8f96f/img_22.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi85OTE0N2NkYmIxYTg2ZmI4ZGExNWExZTQ2NjA0MzI1Zi5tcDQ%3D\",\n" +
            "        \"rid\":\"0e4d0ddf774f0c7761a32a3782d1cc7f\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"image\":\"http://p4.qhimg.com/d/inn/b2a8f96f/img_23.png\",\n" +
            "        \"url\":\"http://tf.play.360kan.com/Object.access/toffee-source-src/L3JlbC92aWRlby9lbi8yMDE3MDYwNi80YjA3Njc0ZWFkMWExYjY2MzU5NGJmYTM4MDZmMmM4NC5tcDQ%3D\",\n" +
            "        \"rid\":\"a9832592e54f4a9d97e95bcee7f3a088\",\n" +
            "        \"watchCount\":\"2.3万\",\n" +
            "        \"duration\":\"04:50\"\n" +
            "    }\n" +
            "]\n";

    public static ArrayList<VideoItemData> getList() {
        return new Gson().fromJson(VIDEO_RES, new TypeToken<List<VideoItemData>>() {
        }.getType());
    }
}

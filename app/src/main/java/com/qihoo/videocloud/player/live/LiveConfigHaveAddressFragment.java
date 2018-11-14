
package com.qihoo.videocloud.player.live;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.qihoo.livecloudrefactor.R;

public class LiveConfigHaveAddressFragment extends Fragment implements View.OnClickListener {

    private RadioGroup rgDecodedMode;
    private RadioButton rbConfigDecodedAuto;
    private RadioButton rbConfigDecodedSoft;
    private ImageView ivPlay;
    private EditText etUrl;
    private EditText etBusinessId;
    private EditText etChannelId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_config_have_address, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rgDecodedMode = (RadioGroup) view.findViewById(R.id.rg_decoded_mode);
        rbConfigDecodedAuto = (RadioButton) view.findViewById(R.id.rb_config_decoded_auto);
        rbConfigDecodedSoft = (RadioButton) view.findViewById(R.id.rb_config_decoded_soft);
        ivPlay = (ImageView) view.findViewById(R.id.iv_play);
        ivPlay.setOnClickListener(this);
        etUrl = (EditText) getView().findViewById(R.id.et_url);
        etBusinessId = (EditText) view.findViewById(R.id.et_busuness_id);
        etChannelId = (EditText) view.findViewById(R.id.et_channel_id);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_play: {
                Intent intent = new Intent(getContext(), LiveActivity.class);
                intent.putExtra("businessId", etBusinessId.getText().toString().trim());
                intent.putExtra("channelId", etChannelId.getText().toString().trim());
                intent.putExtra("haveAddress", Boolean.TRUE);
                intent.putExtra("url", etUrl.getText().toString().trim());
                intent.putExtra("autoDecoded", rbConfigDecodedAuto.isChecked());

                startActivity(intent);
            }
                break;

            default:
                break;
        }
    }
}

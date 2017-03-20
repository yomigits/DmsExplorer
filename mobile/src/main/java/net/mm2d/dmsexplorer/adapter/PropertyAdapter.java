/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.adapter;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.mm2d.dmsexplorer.R;
import net.mm2d.dmsexplorer.databinding.PropertyListItemBinding;
import net.mm2d.dmsexplorer.model.PropertyItemModel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 詳細情報の各項目をRecyclerViewを使用して表示するためのAdapter。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class PropertyAdapter
        extends RecyclerView.Adapter<PropertyAdapter.ViewHolder> {
    protected static final String TITLE_PREFIX = "##";

    public interface OnItemLinkClickListener {
        void onItemLinkClick(String link);
    }

    protected enum Type {
        NORMAL {
            @NonNull
            @Override
            CharSequence format(@NonNull PropertyAdapter adapter, @NonNull String string) {
                return string;
            }
        },
        LINK {
            @NonNull
            @Override
            CharSequence format(@NonNull PropertyAdapter adapter, @NonNull String string) {
                final SpannableString ss = new SpannableString(string);
                ss.setSpan(new LinkSpan(adapter, string), 0, string.length(), Spanned.SPAN_MARK_POINT);
                return ss;
            }
        },
        COMPLEX {
            @NonNull
            @Override
            CharSequence format(@NonNull PropertyAdapter adapter, @NonNull String string) {
                final SpannableStringBuilder builder = new SpannableStringBuilder();
                final String[] lines = string.split("\n");
                for (final String line : lines) {
                    if (line.startsWith(TITLE_PREFIX)) {
                        final int start = builder.length();
                        builder.append(line.substring(2));
                        builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(), Spanned.SPAN_POINT_MARK);
                        builder.append('\n');
                        continue;
                    }
                    final int base = builder.length();
                    builder.append(line);
                    builder.append('\n');
                    final Matcher matcher = URL_PATTERN.matcher(line);
                    while (matcher.find()) {
                        final int start = matcher.start();
                        final int end = matcher.end();
                        builder.setSpan(new LinkSpan(adapter, string.substring(start, end)),
                                start + base, end + base, Spanned.SPAN_POINT_MARK);
                    }
                }
                return builder;
            }
        };

        @NonNull
        abstract CharSequence format(@NonNull PropertyAdapter adapter, @NonNull String string);
    }

    private static class Entry {
        private final String mName;
        private final String mValue;
        private final Type mType;

        Entry(String name, String value, Type type) {
            mName = name;
            mValue = value;
            mType = type;
        }

        String getName() {
            return mName;
        }

        String getValue() {
            return mValue;
        }

        CharSequence getFormatValue(PropertyAdapter adapter) {
            return mType.format(adapter, mValue);
        }
    }

    private final Context mContext;
    private final List<Entry> mList;
    private final LayoutInflater mInflater;
    private OnItemLinkClickListener mListener;

    public PropertyAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mList = new ArrayList<>();
    }

    protected Context getContext() {
        return mContext;
    }

    public void addEntry(String name, String value) {
        addEntry(name, value, Type.NORMAL);
    }

    public void addEntry(String name, String value, Type type) {
        if (value == null || value.isEmpty()) {
            return;
        }
        mList.add(new Entry(name, value, type));
    }

    public void setOnItemLinkClickListener(OnItemLinkClickListener l) {
        mListener = l;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(DataBindingUtil
                .inflate(mInflater, R.layout.property_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.applyItem(this, mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    private static class LinkSpan extends ClickableSpan {
        private final PropertyAdapter mAdapter;
        private final String mLink;

        LinkSpan(PropertyAdapter adapter, String link) {
            mAdapter = adapter;
            mLink = link;
        }

        @Override
        public void onClick(View widget) {
            if (mAdapter.mListener != null) {
                mAdapter.mListener.onItemLinkClick(mLink);
            }
        }
    }

    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://[\\w/:%#\\$&\\?\\(\\)~\\.=\\+\\-]+");

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final PropertyListItemBinding mBinding;

        ViewHolder(PropertyListItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.description.setMovementMethod(LinkMovementMethod.getInstance());
        }

        void applyItem(PropertyAdapter adapter, Entry entry) {
            mBinding.setModel(new PropertyItemModel(entry.getName(), entry.getFormatValue(adapter)));
            mBinding.executePendingBindings();
        }
    }
}
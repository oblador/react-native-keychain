import React, { useState } from 'react';
import {
  Keyboard,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
  useWindowDimensions,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { ModalBottomSheet } from '@swmansion/react-native-bottom-sheet';
import { colors } from '../theme';

type Props = {
  id: string;
  title: string;
  values: string[];
  labels?: string[];
  descriptions?: string[];
  selected: number;
  disabled: boolean;
  onSelect: (index: number) => void;
  last?: boolean;
};

export function OptionPicker({
  id,
  title,
  values,
  labels = values,
  descriptions,
  selected,
  disabled,
  onSelect,
  last,
}: Props) {
  const [open, setOpen] = useState(false);
  const insets = useSafeAreaInsets();
  const { height } = useWindowDimensions();
  return (
    <>
      <Pressable
        testID={`${id}Picker`}
        accessibilityRole="button"
        accessibilityLabel={title}
        accessibilityValue={{ text: labels[selected] }}
        accessibilityHint="Opens available options"
        accessibilityState={{ disabled, expanded: open }}
        disabled={disabled}
        onPress={() => {
          Keyboard.dismiss();
          setOpen(true);
        }}
        style={({ pressed }) => [
          styles.row,
          !last && styles.divider,
          pressed && styles.pressed,
        ]}
      >
        <Text style={styles.label}>{title}</Text>
        <View style={styles.valueGroup}>
          <Text testID={`${id}Value`} style={styles.value}>
            {labels[selected]}
          </Text>
          <Text style={styles.chevron} accessible={false}>
            ›
          </Text>
        </View>
      </Pressable>
      <ModalBottomSheet
        nativeOverlay
        scrimColor="rgba(0, 0, 0, 0.6)"
        index={open ? 1 : 0}
        detents={[0, 'content']}
        onIndexChange={(index) => setOpen(index === 1)}
        onCloseRequest={() => setOpen(false)}
        surface={
          <View style={[StyleSheet.absoluteFill, styles.sheetSurface]} />
        }
      >
        <View style={{ paddingBottom: insets.bottom }} accessibilityViewIsModal>
          <View style={styles.sheetHeader}>
            <Text accessibilityRole="header" style={styles.title}>
              {title}
            </Text>
            <Pressable
              testID={`${id}PickerClose`}
              accessibilityRole="button"
              onPress={() => setOpen(false)}
              style={styles.close}
            >
              <Text style={styles.closeLabel}>Done</Text>
            </Pressable>
          </View>
          <ScrollView
            style={[styles.optionScroll, { maxHeight: height * 0.65 }]}
            bounces={false}
            contentContainerStyle={styles.options}
          >
            {values.map((value, index) => (
              <Pressable
                key={value}
                testID={`${id}-${value}`}
                accessibilityRole="radio"
                accessibilityLabel={labels[index]}
                accessibilityState={{ selected: index === selected }}
                onPress={() => {
                  onSelect(index);
                  setOpen(false);
                }}
                style={({ pressed }) => [
                  styles.option,
                  pressed && styles.pressed,
                ]}
              >
                <View style={styles.optionCopy}>
                  <Text style={styles.optionLabel}>{labels[index]}</Text>
                  {descriptions?.[index] && (
                    <Text style={styles.description}>
                      {descriptions[index]}
                    </Text>
                  )}
                </View>
                <Text style={styles.check} accessible={false}>
                  {index === selected ? '✓' : ''}
                </Text>
              </Pressable>
            ))}
          </ScrollView>
        </View>
      </ModalBottomSheet>
    </>
  );
}

const styles = StyleSheet.create({
  row: {
    minHeight: 54,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 16,
  },
  divider: {
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.raised,
  },
  label: { color: colors.text, fontSize: 14, flexShrink: 1 },
  valueGroup: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    flexShrink: 1,
  },
  value: {
    color: colors.muted,
    fontSize: 14,
    textAlign: 'right',
    flexShrink: 1,
  },
  chevron: { color: colors.faint, fontSize: 22 },
  pressed: { opacity: 0.6 },
  sheetSurface: {
    backgroundColor: colors.surface,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
  },
  sheetHeader: {
    paddingLeft: 24,
    paddingRight: 12,
    paddingTop: 12,
    paddingBottom: 8,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: { color: colors.text, fontSize: 20, fontWeight: '600' },
  close: {
    minHeight: 44,
    minWidth: 64,
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeLabel: { color: colors.muted, fontSize: 14, fontWeight: '500' },
  optionScroll: { flexGrow: 0 },
  options: { paddingHorizontal: 24, paddingBottom: 16 },
  option: {
    minHeight: 64,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
    paddingVertical: 14,
  },
  optionCopy: { flex: 1, gap: 4 },
  optionLabel: { color: colors.text, fontSize: 16 },
  description: { color: colors.muted, fontSize: 13, lineHeight: 18 },
  check: { color: colors.text, fontSize: 20, width: 24, textAlign: 'right' },
});

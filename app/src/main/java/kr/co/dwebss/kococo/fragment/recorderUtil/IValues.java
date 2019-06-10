package kr.co.dwebss.kococo.fragment.recorderUtil;

public interface IValues<T> extends ICleanable {
    Class<T> getValuesType();

    int size();
}

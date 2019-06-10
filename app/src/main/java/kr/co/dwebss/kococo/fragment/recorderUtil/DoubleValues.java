package kr.co.dwebss.kococo.fragment.recorderUtil;

public class DoubleValues  implements IValues<Double> {
    private double[] a;
    private int b;

    public DoubleValues() {
        this.a = new double[0];
    }

    public DoubleValues(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0");
        } else {
            this.a = new double[capacity];
        }
    }

    public DoubleValues(double[] items) {
        this.a = items;
        this.b = items.length;
    }

    public double[] getItemsArray() {
        return this.a;
    }

    public void add(double value) {
        this.a(this.b + 1);
        this.a[this.b++] = value;
    }

    public void add(int location, double value) {
        if (location > this.b) {
            throw new ArrayIndexOutOfBoundsException("location");
        } else {
            this.a(this.b + 1);
            if (location < this.b) {
                System.arraycopy(this.a, location, this.a, location + 1, this.b - location);
            }

            this.a[location] = value;
            ++this.b;
        }
    }

    public void add(double[] values) {
        this.add(values, 0, values.length);
    }

    public void add(double[] values, int startIndex, int count) {
        this.a(this.b + count);
        System.arraycopy(values, startIndex, this.a, this.b, count);
        this.b += count;
    }

    private void a(int var1) {
        if (this.a.length < var1) {
            int var2 = this.a.length == 0 ? 4 : this.a.length * 2;
            if (var2 < var1) {
                var2 = var1;
            }

            this.b(var2);
        }

    }

    private void b(int var1) {
        if (var1 < this.b) {
            throw new IllegalArgumentException("capacity");
        } else {
            if (var1 != this.b) {
                if (var1 > 0) {
                    double[] var2 = new double[var1];
                    if (this.b > 0) {
                        System.arraycopy(this.a, 0, var2, 0, this.b);
                    }

                    this.a = var2;
                    return;
                }

                this.a = new double[0];
            }

        }
    }

    public void set(int location, double value) {
        if (location >= this.b) {
            throw new ArrayIndexOutOfBoundsException("location");
        } else {
            this.a[location] = value;
        }
    }

    public double get(int index) {
        if (index >= this.b) {
            throw new ArrayIndexOutOfBoundsException("index");
        } else {
            return this.a[index];
        }
    }

    public int size() {
        return this.b;
    }

    public void setSize(int size) {
        this.a(size);
        this.b = size;
    }

    public void remove(int location) {
        if (location >= this.b) {
            throw new ArrayIndexOutOfBoundsException("location");
        } else {
            --this.b;
            System.arraycopy(this.a, location + 1, this.a, location, this.b - location);
        }
    }

    public void clear() {
        this.b = 0;
    }

    public void disposeItems() {
        this.clear();
        this.a = new double[0];
    }

    public Class<Double> getValuesType() {
        return Double.class;
    }
}

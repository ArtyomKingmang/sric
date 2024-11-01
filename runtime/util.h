
#ifndef SRIC_UTIL_H_
#define SRIC_UTIL_H_

#include "Ptr.h"
#include "Refable.h"

namespace sric {


	template<typename T>
	T* unsafeAlloc() {
		return new T();
	}

	template<typename T>
	void unsafeFree(T* p) {
		delete p;
	}

	template<typename T>
	bool ptrIs(void* p) {
		return dynamic_cast<T*>(p) != nullptr;
	}

	template<typename T, typename F>
	bool ptrIs(F& p) {
		return dynamic_cast<T*>(p.get()) != nullptr;
	}

	template<typename T>
	T* nonNullable(T* p) {
		sric::sc_assert(p != nullptr, "Non-Nullable");
		return p;
	}

	template<typename T>
	OwnPtr<T>& nonNullable(OwnPtr<T>& p) {
		sric::sc_assert(!p.isNull(), "Non-Nullable");
		return p;
	}

	template<typename T>
	RefPtr<T> nonNullable(RefPtr<T> p) {
		sric::sc_assert(!p.isNull(), "Non-Nullable");
		return p;
	}

	template<typename T>
	int hashCode(const T& p) {
		return p.hashCode();
	}

	template<typename T>
	int hashCode(const RefPtr<T> p) {
		return p->hashCode();
	}

	template<typename T>
	int hashCode(int p) {
		return p;
	}

	template<typename T>
	int compare(const T& a, const T& b) {
		return a.compare(RefPtr<T>(const_cast<T*>(&b)));
	}

	template<typename T>
	int compare(const RefPtr<T> a, const RefPtr<T> b) {
		return a->compare(b);
	}

	template<typename T>
	int compare(int a, int b) {
		return a - b;
	}
}
//
//inline bool isNull(void* p) {
//	return p == nullptr;
//}

#endif